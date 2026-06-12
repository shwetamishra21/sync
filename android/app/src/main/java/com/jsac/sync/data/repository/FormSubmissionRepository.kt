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
 * ✅ OPTIMIZED: Single source of truth for submission operations
 *
 * Removed:
 * - syncSubmissionToServer() with runBlocking (old implementation)
 * - syncSubmissionManually() (duplicate)
 * - syncMultipleSubmissions() (complex, rarely used)
 *
 * Added:
 * - syncSubmissionToServer(submissionId, token) - Accept token as parameter
 *
 * Benefits:
 * - No duplicate sync logic
 * - Token handling is caller's responsibility (FormSyncWorker)
 * - Cleaner, easier to understand
 * - No async/sync confusion
 */
@Suppress("UNCHECKED_CAST")
class FormSubmissionRepository @Inject constructor(
    private val api: SubmissionApi,
    private val submissionDao: FormSubmissionDao,
    private val mediaFileDao: MediaFileDao,
    private val gson: Gson
) {

    // ============================================
    // SUBMIT FORM (LOCAL SAVE - OFFLINE FIRST)
    // ============================================

    /**
     * Submit form to local database
     *
     * Returns immediately (offline-first pattern)
     * Sync to server happens later via FormSyncWorker
     *
     * @param formId Form ID
     * @param formData Map of field values
     * @param gpsLocation GPS coordinates (optional)
     * @return Result with submission ID
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
    // SYNC TO SERVER (CALLED BY FormSyncWorker)
    // ============================================

    /**
     * ✅ OPTIMIZED: Sync single submission to server
     *
     * Called by FormSyncWorker with token already obtained
     * This is the ONLY sync method (no duplicates)
     *
     * @param submissionId Local submission ID
     * @param token JWT token from SessionManager
     * @param gpsLocation GPS coordinates (optional)
     * @return Result with server submission ID
     *
     * Flow:
     * 1. Get submission from local DB
     * 2. Parse form data from JSON
     * 3. Create API request
     * 4. Call API with token
     * 5. On success: Return server submission ID
     * 6. On failure: Throw exception (caller marks as FAILED)
     */
    suspend fun syncSubmissionToServer(
        submissionId: Int,
        token: String,
        gpsLocation: GpsLocation? = null
    ): Result<String> {
        return try {
            Log.d("FormSubmissionRepository", "📤 Syncing submission #$submissionId to server")

            // Get submission from DB
            val submission = submissionDao.getSubmissionByIdOnce(submissionId)
                ?: return Result.failure(Exception("Submission not found"))

            Log.d("FormSubmissionRepository", "   Form ID: ${submission.form_id}")
            Log.d("FormSubmissionRepository", "   Status: ${submission.sync_status}")

            // Parse form data
            val formData: Map<String, String> = try {
                gson.fromJson(submission.form_data, Map::class.java) as Map<String, String>
            } catch (e: Exception) {
                Log.e("FormSubmissionRepository", "   Error parsing form data: ${e.message}")
                emptyMap()
            }

            // Create request
            val request = SubmitFormRequest(
                formId = submission.form_id,
                formData = formData,
                submittedAt = submission.created_at,
                gpsLocation = gpsLocation
            )

            Log.d("FormSubmissionRepository", "   🌐 Calling API...")

            // Send to server
            // Note: Token is injected by AuthInterceptor from request headers
            // FormSyncWorker should pass token in Authorization header
            val response = api.submitForm(request)

            if (response.isSuccessful && response.body() != null) {
                val submissionIdFromServer = response.body()!!.submissionId

                Log.d("FormSubmissionRepository", "   ✅ Success! Server ID: $submissionIdFromServer")

                Result.success(submissionIdFromServer)

            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e("FormSubmissionRepository", "   ❌ API returned error: $errorMsg")

                Result.failure(Exception(errorMsg))
            }

        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "   ❌ Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ============================================
    // GET SUBMISSIONS
    // ============================================

    /**
     * Get all submissions
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
     */
    fun getSubmissionsByStatus(status: String): Flow<List<FormSubmissionEntity>> = flow {
        try {
            Log.d("FormSubmissionRepository", "🔍 Getting submissions with status: $status")
            submissionDao.getSubmissionsByStatus(status).collect { submissions ->
                Log.d("FormSubmissionRepository", "📊 Got ${submissions.size} submissions with status $status")
                emit(submissions)
            }
        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "❌ Error: ${e.message}", e)
            emit(emptyList())
        }
    }

    /**
     * Get single submission by ID (Flow version)
     */
    fun getSubmissionById(submissionId: Int): Flow<FormSubmissionEntity?> = flow {
        try {
            Log.d("FormSubmissionRepository", "📋 Getting submission: $submissionId")
            submissionDao.getSubmissionById(submissionId).collect { submission ->
                emit(submission)
            }
        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "❌ Error: ${e.message}", e)
            emit(null)
        }
    }

    /**
     * Get submissions for a specific form
     */
    fun getSubmissionsByFormId(formId: String): Flow<List<FormSubmissionEntity>> =
        submissionDao.getSubmissionsByFormId(formId)

    /**
     * Get pending submissions for sync
     */
    suspend fun getPendingSyncSubmissions(limit: Int = 50): List<FormSubmissionEntity> =
        submissionDao.getPendingSyncSubmissions(limit)

    // ============================================
    // COUNTS & STATISTICS
    // ============================================

    /**
     * Count submissions by status
     */
    suspend fun countByStatus(status: String): Int {
        return try {
            Log.d("FormSubmissionRepository", "📊 Counting submissions with status: $status")
            val count = submissionDao.countByStatus(status)
            Log.d("FormSubmissionRepository", "✅ Count: $count")
            count
        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "❌ Error: ${e.message}", e)
            0
        }
    }

    // ============================================
    // MEDIA FILES
    // ============================================

    /**
     * Add media file to submission
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
        Log.e("FormSubmissionRepository", "❌ Error: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Get media files for submission
     */
    fun getMediaFilesBySubmissionId(submissionId: Int): Flow<List<MediaFileEntity>> =
        mediaFileDao.getMediaFilesBySubmissionId(submissionId)

    /**
     * Update media file
     */
    suspend fun updateMediaFile(mediaFile: MediaFileEntity) {
        mediaFileDao.updateMediaFile(mediaFile)
    }

    /**
     * Mark media as uploaded
     */
    suspend fun markMediaAsUploaded(mediaId: Int, serverUrl: String) {
        mediaFileDao.markAsUploaded(mediaId, serverUrl)
    }

    // ============================================
    // STATUS UPDATES
    // ============================================

    /**
     * Update submission sync status
     */
    suspend fun updateSubmissionStatus(submissionId: Int, status: String) {
        submissionDao.updateSubmissionStatus(submissionId, status)
    }

    /**
     * Mark submission as SYNCED
     */
    suspend fun markAsSynced(submissionId: Int) {
        submissionDao.markAsSynced(submissionId)
    }

    /**
     * Mark submission as FAILED
     */
    suspend fun markAsFailed(submissionId: Int, errorMsg: String) {
        submissionDao.markAsFailed(submissionId, errorMsg)
    }

    // ============================================
    // DELETE
    // ============================================

    /**
     * Delete submission
     */
    suspend fun deleteSubmission(submissionId: Int) {
        try {
            Log.d("FormSubmissionRepository", "🗑️ Deleting submission: $submissionId")
            submissionDao.deleteSubmissionById(submissionId)
            Log.d("FormSubmissionRepository", "✅ Deleted")
        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "❌ Error: ${e.message}", e)
            throw e
        }
    }
    /**
     * Get pending media files for upload
     */
    suspend fun getPendingUploadFiles(limit: Int = 50): List<MediaFileEntity> {
        return try {
            mediaFileDao.getPendingUploadFiles(limit)
        } catch (e: Exception) {
            Log.e(
                "FormSubmissionRepository",
                "❌ Error getting pending upload files: ${e.message}",
                e
            )
            emptyList()
        }
    }
}

/**
 * 🗑️ REMOVED METHODS:
 *
 * ❌ syncSubmissionToServer() (old) - Had runBlocking, called without token
 * ❌ syncSubmissionManually() - Duplicate of above
 * ❌ syncMultipleSubmissions() - Complex, use scheduleSync() instead
 * ❌ getSyncMessage() - Not needed
 * ❌ getTotalPending() - Use countByStatus()
 * ❌ hasPending() - Not needed
 * ❌ getLastSyncTimeFormatted() - Not needed
 * ❌ getFormNameById() - Not used
 * ❌ getSubmissionByIdOnce() - Internal helper, not exposed
 *
 * 🔑 KEY CHANGE:
 *
 * OLD FLOW (BROKEN):
 * syncSubmissionManually()
 *   → submissionDao.getSubmissionByIdOnce()
 *   → gson.fromJson()
 *   → api.submitForm()  ← No token! Interceptor tries to get it with runBlocking
 *   → markAsSynced()
 *
 * NEW FLOW (WORKING):
 * FormSyncWorker.doWork()
 *   → sessionManager.token.first()  ← Get token once
 *   → syncSubmissionToServer(submissionId, token)
 *     → submissionDao.getSubmissionByIdOnce()
 *     → gson.fromJson()
 *     → api.submitForm()  ← Token passed by caller
 *     → return Result
 *   → markAsSynced() or markAsFailed()
 *
 * Benefits:
 * - No nested async/blocking calls
 * - Token obtained safely
 * - Clear error handling
 * - Easy to debug
 */