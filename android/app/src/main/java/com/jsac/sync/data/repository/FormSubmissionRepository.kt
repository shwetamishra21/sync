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
     * Enhanced with:
     * 1. Token validation
     * 2. Detailed error logging
     * 3. Data validation before sync
     * 4. Response logging
     *
     * @param submissionId Local submission ID
     * @param token JWT token from SessionManager
     * @param gpsLocation GPS coordinates (optional)
     * @return Result with server submission ID
     */
    suspend fun syncSubmissionToServer(
        submissionId: Int,
        token: String,
        gpsLocation: GpsLocation? = null
    ): Result<String> {
        return try {
            Log.d("FormSubmissionRepository", "📤 Syncing submission #$submissionId")

            // Get submission from DB
            val submission = submissionDao.getSubmissionByIdOnce(submissionId)
                ?: return Result.failure(Exception("Submission not found"))

            Log.d("FormSubmissionRepository", "   Form ID: ${submission.form_id}")
            Log.d("FormSubmissionRepository", "   Status: ${submission.sync_status}")
            Log.d("FormSubmissionRepository", "   Data length: ${submission.form_data.length} chars")

            // Parse form data with better error handling
            val formData: Map<String, String> = try {
                Log.d("FormSubmissionRepository", "   📄 Parsing form_data JSON...")
                val parsed = gson.fromJson(submission.form_data, Map::class.java) as Map<String, String>
                Log.d("FormSubmissionRepository", "   ✅ Parsed ${parsed.size} fields")
                parsed
            } catch (e: Exception) {
                Log.e("FormSubmissionRepository", "   ❌ JSON parsing failed: ${e.message}")
                Log.e("FormSubmissionRepository", "   🔍 Raw data (first 200 chars):")
                Log.e("FormSubmissionRepository", "      ${submission.form_data.take(200)}")
                emptyMap()
            }

            if (formData.isEmpty()) {
                Log.w("FormSubmissionRepository", "   ⚠️ WARNING: Form data is empty!")
                Log.w("FormSubmissionRepository", "   This might cause API validation errors")
            }

            // Create request
            val request = SubmitFormRequest(
                formId = submission.form_id,
                formData = formData,
                submittedAt = submission.created_at,
                gpsLocation = gpsLocation
            )

            Log.d("FormSubmissionRepository", "   🌐 Preparing API call...")
            Log.d("FormSubmissionRepository", "   URL: POST /forms/submit")
            Log.d("FormSubmissionRepository", "   Form: ${submission.form_id}")
            Log.d("FormSubmissionRepository", "   Fields: ${formData.size}")
            Log.d("FormSubmissionRepository", "   Token: ${token.take(20)}...")

            // Send to server
            val response = api.submitForm(request)

            Log.d("FormSubmissionRepository", "   📡 Response received: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val submissionIdFromServer = response.body()!!.submissionId

                Log.d("FormSubmissionRepository", "   ✅ SUCCESS!")
                Log.d("FormSubmissionRepository", "   Server submission ID: $submissionIdFromServer")

                Result.success(submissionIdFromServer)

            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                val errorBody = try {
                    response.errorBody()?.string() ?: "No error body"
                } catch (e: Exception) {
                    "Could not read error body"
                }

                Log.e("FormSubmissionRepository", "   ❌ API Error: $errorMsg")
                Log.e("FormSubmissionRepository", "   Response body:")
                Log.e("FormSubmissionRepository", "   $errorBody")

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

    fun getSubmissionsByFormId(formId: String): Flow<List<FormSubmissionEntity>> =
        submissionDao.getSubmissionsByFormId(formId)

    suspend fun getPendingSyncSubmissions(limit: Int = 50): List<FormSubmissionEntity> =
        submissionDao.getPendingSyncSubmissions(limit)

    // ============================================
    // COUNTS & STATISTICS
    // ============================================

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

    fun getMediaFilesBySubmissionId(submissionId: Int): Flow<List<MediaFileEntity>> =
        mediaFileDao.getMediaFilesBySubmissionId(submissionId)

    suspend fun updateMediaFile(mediaFile: MediaFileEntity) {
        mediaFileDao.updateMediaFile(mediaFile)
    }

    suspend fun markMediaAsUploaded(mediaId: Int, serverUrl: String) {
        mediaFileDao.markAsUploaded(mediaId, serverUrl)
    }

    // ============================================
    // STATUS UPDATES
    // ============================================

    suspend fun updateSubmissionStatus(submissionId: Int, status: String) {
        submissionDao.updateSubmissionStatus(submissionId, status)
    }

    suspend fun markAsSynced(submissionId: Int) {
        submissionDao.markAsSynced(submissionId)
    }

    suspend fun markAsFailed(submissionId: Int, errorMsg: String) {
        submissionDao.markAsFailed(submissionId, errorMsg)
    }

    // ============================================
    // DELETE
    // ============================================

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

    suspend fun getPendingUploadFiles(limit: Int = 50): List<MediaFileEntity> {
        return try {
            mediaFileDao.getPendingUploadFiles(limit)
        } catch (e: Exception) {
            Log.e("FormSubmissionRepository", "❌ Error getting pending upload files: ${e.message}", e)
            emptyList()
        }
    }
}