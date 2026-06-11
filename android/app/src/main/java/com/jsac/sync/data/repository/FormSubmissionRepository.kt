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
     * Get all submissions
     * Ordered by creation date (newest first)
     */
    fun getAllSubmissions(): Flow<List<FormSubmissionEntity>> = flow {
        try {
            submissionDao.getAllSubmissions().collect { submissions ->
                Log.d("FormSubmissionRepository", "📋 Got ${submissions.size} submissions")
                emit(submissions)
            }
        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "❌ Error getting all submissions: ${e.message}", e)
            emit(emptyList())
        }
    }

    /**
     * Get submissions filtered by sync status
     * @param status PENDING, SYNCING, SYNCED, or FAILED
     */
    fun getSubmissionsByStatus(status: String): Flow<List<FormSubmissionEntity>> = flow {
        try {
            Log.d("FormSubmissionRepository", "🔍 Getting submissions with status: $status")
            submissionDao.getSubmissionsByStatus(status).collect { submissions ->
                Log.d("FormSubmissionRepository", "📊 Got ${submissions.size} submissions with status $status")
                emit(submissions)
            }
        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "❌ Error getting submissions by status: ${e.message}", e)
            emit(emptyList())
        }
    }

    /**
     * Get a single submission by ID (Flow version)
     */
    fun getSubmissionById(submissionId: Int): Flow<FormSubmissionEntity?> = flow {
        try {
            Log.d("FormSubmissionRepository", "📋 Getting submission: $submissionId")
            submissionDao.getSubmissionById(submissionId).collect { submission ->
                emit(submission)
            }
        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "❌ Error getting submission: ${e.message}", e)
            emit(null)
        }
    }

    /**
     * Get a specific submission from local DB
     */
    fun getSubmissionsByFormId(formId: String): Flow<List<FormSubmissionEntity>> =
        submissionDao.getSubmissionsByFormId(formId)

    /**
     * Get all pending submissions for sync
     */
    suspend fun getPendingSyncSubmissions(limit: Int = 50): List<FormSubmissionEntity> =
        submissionDao.getPendingSyncSubmissions(limit)

    /**
     * Get count of submissions by status
     */
    suspend fun countByStatus(status: String): Int {
        return try {
            Log.d("FormSubmissionRepository", "📊 Counting submissions with status: $status")
            val count = submissionDao.countByStatus(status)
            Log.d("FormSubmissionRepository", "✅ Count: $count")
            count
        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "❌ Error counting submissions: ${e.message}", e)
            0
        }
    }

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
            val submission = submissionDao.getSubmissionByIdOnce(submissionId)
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
    // MANUAL SYNC - NEW METHODS
    // ============================================

    /**
     * Sync a submission to server (manual sync from UI)
     *
     * @param submissionId ID of submission to sync
     * @return Result with submission ID from server or error
     */
    suspend fun syncSubmissionManually(submissionId: Int): Result<String> {
        Log.d("FormSubmissionRepository", "⚡ Manual sync for submission: $submissionId")

        return try {
            // Get submission
            val submission = submissionDao.getSubmissionByIdOnce(submissionId)
                ?: return Result.failure(Exception("Submission not found"))

            // Mark as syncing
            submissionDao.updateSubmissionStatus(submissionId, "SYNCING")

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
                gpsLocation = null
            )

            // Send to server
            val response = api.submitForm(request)

            if (response.isSuccessful && response.body() != null) {
                val submissionIdFromServer = response.body()!!.submissionId

                // Mark as synced
                submissionDao.markAsSynced(submissionId)

                Log.d("FormSubmissionRepository", "✅ Manual sync successful: $submissionIdFromServer")
                Result.success(submissionIdFromServer)

            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                submissionDao.markAsFailed(submissionId, errorMsg)

                Log.e("FormSubmissionRepository", "❌ Manual sync failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }

        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "❌ Exception during manual sync: ${e.message}", e)
            try {
                submissionDao.markAsFailed(submissionId, e.message ?: "Unknown error")
            } catch (updateError: Exception) {
                Log.e("FormSubmissionRepository", "Error updating status: ${updateError.message}")
            }
            Result.failure(e)
        }
    }

    /**
     * Sync multiple submissions (batch sync from UI)
     *
     * @param submissionIds List of submission IDs to sync
     * @return Result with sync stats (successful, failed counts)
     */
    suspend fun syncMultipleSubmissions(
        submissionIds: List<Int>
    ): Result<SyncResult> {

        Log.d(
            "FormSubmissionRepository",
            "⚡ Batch sync for ${submissionIds.size} submissions"
        )

        var successCount = 0
        var failureCount = 0
        val failedIds = mutableListOf<Int>()

        return try {

            for (submissionId in submissionIds) {

                val result = syncSubmissionManually(submissionId)

                result.onSuccess {
                    successCount++
                }

                result.onFailure {
                    failureCount++
                    failedIds.add(submissionId)
                }
            }

            Result.success(
                SyncResult(
                    total = submissionIds.size,
                    successful = successCount,
                    failed = failureCount,
                    failedIds = failedIds
                )
            )

        } catch (e: Exception) {
            Log.e(
                "FormSubmissionRepository",
                "❌ Batch sync error: ${e.message}",
                e
            )

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

    // ============================================
    // DELETE - NEW METHOD
    // ============================================

    /**
     * Delete a submission
     */
    suspend fun deleteSubmission(submissionId: Int) {
        try {
            Log.d("FormSubmissionRepository", "🗑️ Deleting submission: $submissionId")
            submissionDao.deleteSubmissionById(submissionId)
            Log.d("FormSubmissionRepository", "✅ Deleted successfully")
        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "❌ Error deleting: ${e.message}", e)
            throw e
        }
    }

    // ============================================
    // HELPER
    // ============================================

    /**
     * Get submission by ID (non-Flow version for internal use)
     */
    private suspend fun getSubmissionByIdOnce(submissionId: Int): FormSubmissionEntity? {
        var result: FormSubmissionEntity? = null
        submissionDao.getSubmissionById(submissionId).collect { result = it }
        return result
    }

    /**
     * Get form name for a submission
     */
    suspend fun getFormNameById(formId: String): String {
        Log.d("FormSubmissionRepository", "📋 Getting form name for: $formId")
        return try {
            // Return form ID as name if no separate Form entity exists
            // TODO: Replace with actual form name query if Form table exists
            formId
        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "Error getting form name: ${e.message}")
            formId
        }
    }

    // ============================================
    // DATA CLASS FOR SYNC RESULT
    // ============================================

    /**
     * Result of batch sync operation
     */
    data class SyncResult(
        val total: Int,
        val successful: Int,
        val failed: Int,
        val failedIds: List<Int> = emptyList()
    ) {
        override fun toString(): String {
            return "$successful/$total synced successfully" +
                    if (failed > 0) ", $failed failed" else ""
        }
    }
}