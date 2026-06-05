package com.jsac.sync.data.repository

import android.util.Log
import com.google.gson.Gson
import com.jsac.sync.data.local.db.dao.FormSubmissionDao
import com.jsac.sync.data.local.db.dao.MediaFileDao
import com.jsac.sync.data.local.db.entity.FormSubmissionEntity
import com.jsac.sync.data.local.db.entity.MediaFileEntity
import com.jsac.sync.data.local.db.entity.SyncStatus
import com.jsac.sync.data.local.db.entity.UploadStatus
import com.jsac.sync.data.remote.api.SubmissionApi
import com.jsac.sync.data.remote.dto.GpsLocation
import com.jsac.sync.data.remote.dto.SubmitFormRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Repository for form submissions
 * Implements offline-first pattern:
 * 1. Save form locally
 * 2. Return success immediately
 * 3. Sync to server when online (handled by WorkManager)
 */
class FormSubmissionRepository @Inject constructor(
    private val api: SubmissionApi,
    private val submissionDao: FormSubmissionDao,
    private val mediaFileDao: MediaFileDao,
    private val gson: Gson
) {

    // ============================================
    // SUBMIT FORM (OFFLINE-FIRST)
    // ============================================

    /**
     * Submit a form - saves locally first, returns immediately
     * Server sync happens later via WorkManager
     */
    suspend fun submitForm(
        formId: String,
        formData: Map<String, String>,
        gpsLocation: GpsLocation? = null
    ): Result<Int> = try {
        Log.d("FormSubmissionRepository", "📝 Submitting form: $formId")

        val submission = FormSubmissionEntity(
            form_id = formId,
            form_data = gson.toJson(formData),
            sync_status = SyncStatus.PENDING,
            created_at = System.currentTimeMillis(),
            updated_at = System.currentTimeMillis()
        )

        val submissionId = submissionDao.insertSubmission(submission).toInt()

        Log.d("FormSubmissionRepository", "✅ Form saved locally - ID: $submissionId")

        Result.success(submissionId)

    } catch (e: Exception) {
        Log.e("FormSubmissionRepository", "❌ Error submitting form: ${e.message}", e)
        Result.failure(e)
    }

    // ============================================
    // ADD MEDIA FILE TO SUBMISSION
    // ============================================

    /**
     * Add a media file (photo/document) to a submission
     */
    suspend fun addMediaFile(
        submissionId: Int,
        fieldId: String,
        localPath: String,
        fileName: String,
        fileSize: Long,
        fileType: String
    ): Result<Int> = try {
        Log.d("FormSubmissionRepository", "📸 Adding media file: $fileName")

        val mediaFile = MediaFileEntity(
            submission_id = submissionId,
            field_id = fieldId,
            local_path = localPath,
            file_name = fileName,
            file_size = fileSize,
            file_type = fileType,
            upload_status = UploadStatus.LOCAL,
            created_at = System.currentTimeMillis()
        )

        val mediaId = mediaFileDao.insertMediaFile(mediaFile).toInt()

        Log.d("FormSubmissionRepository", "✅ Media file saved - ID: $mediaId")

        Result.success(mediaId)

    } catch (e: Exception) {
        Log.e("FormSubmissionRepository", "❌ Error adding media file: ${e.message}", e)
        Result.failure(e)
    }

    // ============================================
    // GET SUBMISSIONS
    // ============================================

    /**
     * Get a specific submission from local DB
     */
    fun getSubmissionById(submissionId: Int): Flow<FormSubmissionEntity?> =
        submissionDao.getSubmissionById(submissionId)

    /**
     * Get all submissions for a form
     */
    fun getSubmissionsByFormId(formId: String): Flow<List<FormSubmissionEntity>> =
        submissionDao.getSubmissionsByFormId(formId)

    /**
     * Get submissions by sync status
     */
    fun getSubmissionsByStatus(status: String): Flow<List<FormSubmissionEntity>> =
        submissionDao.getSubmissionsByStatus(status)

    /**
     * Get all pending submissions for sync
     */
    suspend fun getPendingSyncSubmissions(limit: Int = 50): List<FormSubmissionEntity> =
        submissionDao.getPendingSyncSubmissions(limit)

    /**
     * Count submissions by status
     * Used for showing badge counts in UI
     */
    suspend fun countByStatus(status: String): Int =
        submissionDao.countByStatus(status)

    // ============================================
    // GET MEDIA FILES
    // ============================================

    /**
     * Get all media files for a submission
     */
    fun getMediaFilesBySubmissionId(submissionId: Int): Flow<List<MediaFileEntity>> =
        mediaFileDao.getMediaFilesBySubmissionId(submissionId)

    /**
     * Get pending upload files
     */
    suspend fun getPendingUploadFiles(limit: Int = 20): List<MediaFileEntity> =
        mediaFileDao.getPendingUploadFiles(limit)

    /**
     * Update a media file (public wrapper for worker access)
     */
    suspend fun updateMediaFile(mediaFile: MediaFileEntity) {
        mediaFileDao.updateMediaFile(mediaFile)
    }

    // ============================================
    // SYNC OPERATIONS (Called by WorkManager)
    // ============================================

    /**
     * Sync a submission to the server
     * Called by background sync worker
     */
    suspend fun syncSubmissionToServer(
        submissionId: Int,
        gpsLocation: GpsLocation? = null
    ): Result<String> {
        return try {
            val submission = getSubmissionByIdOnce(submissionId)
                ?: return Result.failure(Exception("Submission not found"))

            // Mark as syncing
            submissionDao.updateSubmissionStatus(submissionId, SyncStatus.SYNCING)

            // Parse form data
            val formData: Map<String, String> = try {
                gson.fromJson(submission.form_data, Map::class.java) as Map<String, String>
            } catch (e: Exception) {
                Log.e("FormSubmissionRepository", "Error parsing form data: ${e.message}")
                emptyMap()
            }

            // Create request
            val request = SubmitFormRequest(
                formId = submission.form_id,
                formData = formData,
                submittedAt = submission.created_at,
                gpsLocation = gpsLocation
            )

            // Send to server
            val response = api.submitForm(request)

            if (response.isSuccessful && response.body() != null) {
                val submissionIdFromServer = response.body()!!.submissionId

                // Mark as synced
                submissionDao.markAsSynced(submissionId)

                Log.d("FormSubmissionRepository", "✅ Submission synced: $submissionIdFromServer")

                Result.success(submissionIdFromServer)

            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                submissionDao.markAsFailed(submissionId, errorMsg)

                Log.e("FormSubmissionRepository", "❌ Sync failed: $errorMsg")

                Result.failure(Exception(errorMsg))
            }

        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "❌ Exception during sync: ${e.message}", e)
            submissionDao.markAsFailed(submissionId, e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    // ============================================
    // UPDATE OPERATIONS
    // ============================================

    /**
     * Update submission sync status
     */
    suspend fun updateSubmissionStatus(submissionId: Int, status: String) {
        submissionDao.updateSubmissionStatus(submissionId, status)
    }

    /**
     * Mark media file as uploaded
     */
    suspend fun markMediaAsUploaded(mediaId: Int, serverUrl: String) {
        mediaFileDao.markAsUploaded(mediaId, serverUrl)
    }

    /**
     * Delete a submission (and its media files via cascade)
     */
    suspend fun deleteSubmission(submissionId: Int) {
        submissionDao.deleteSubmissionById(submissionId)
    }

    // ============================================
    // HELPER
    // ============================================

    /**
     * Get submission by ID (non-Flow version for internal use)
     * Uses block body to properly handle the Flow collection
     */
    private suspend fun getSubmissionByIdOnce(submissionId: Int): FormSubmissionEntity? {
        var result: FormSubmissionEntity? = null
        submissionDao.getSubmissionById(submissionId).collect { result = it }
        return result
    }
}