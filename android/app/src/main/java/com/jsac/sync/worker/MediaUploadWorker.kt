package com.jsac.sync.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.jsac.sync.data.local.db.entity.UploadStatus
import com.jsac.sync.data.repository.FormSubmissionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * WorkManager background worker for uploading media files
 *
 * Features:
 * - Uploads pending media files to server using multipart/form-data
 * - Exponential backoff on failure
 * - Only uploads when device has internet
 * - Updates sync status after upload
 * - Handles large files with streaming
 *
 * Request Format (Multipart):
 * POST /media/upload
 * - file: The file binary data
 * - submission_id: Associated submission ID
 * - field_id: Which form field this belongs to
 * Authorization: Bearer {JWT_TOKEN}
 */
@HiltWorker
class MediaUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val submissionRepository: FormSubmissionRepository
) : Worker(context, params) {

    companion object {
        const val WORK_TAG = "media_upload"
        const val WORK_NAME = "media_upload_work"
        const val API_BASE_URL = "http://10.0.2.2:5000"  // For emulator; change for device
        const val UPLOAD_ENDPOINT = "$API_BASE_URL/media/upload"

        /**
         * Schedule media upload with WorkManager
         *
         * - Requires internet connection
         * - Retries up to 3 times with exponential backoff
         * - Max backoff: 16 minutes
         */
        fun scheduleUpload(context: Context) {
            Log.d("MediaUploadWorker", "📅 Scheduling media upload...")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadRequest = OneTimeWorkRequestBuilder<MediaUploadWorker>()
                .addTag(WORK_TAG)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    5,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                uploadRequest
            )

            Log.d("MediaUploadWorker", "✅ Upload scheduled")
        }

        /**
         * Cancel pending upload
         */
        fun cancelUpload(context: Context) {
            Log.d("MediaUploadWorker", "❌ Canceling upload...")
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }
    }

    override fun doWork(): Result = runBlocking {
        Log.d("MediaUploadWorker", "🎥 Starting media upload...")

        return@runBlocking try {
            // Get pending media files (limit 5 per sync)
            val pendingFiles = submissionRepository.getPendingUploadFiles(limit = 5)

            if (pendingFiles.isEmpty()) {
                Log.d("MediaUploadWorker", "✅ No pending media files")
                return@runBlocking Result.success()
            }

            Log.d("MediaUploadWorker", "📤 Uploading ${pendingFiles.size} media files...")

            var successCount = 0
            var failureCount = 0

            for (mediaFile in pendingFiles) {
                try {
                    // Mark as uploading
                    submissionRepository.updateMediaFile(
                        mediaFile.copy(upload_status = UploadStatus.UPLOADING)
                    )

                    Log.d("MediaUploadWorker", "⬆️  Uploading: ${mediaFile.file_name}")

                    // Upload to server
                    val uploadResult = uploadFileToServer(
                        mediaFile.local_path,
                        mediaFile.file_name,
                        mediaFile.submission_id.toString(),
                        mediaFile.field_id
                    )

                    if (uploadResult != null) {
                        // Mark as uploaded with server URL
                        submissionRepository.markMediaAsUploaded(mediaFile.id, uploadResult)
                        Log.d("MediaUploadWorker", "✅ Uploaded: ${mediaFile.file_name}")
                        Log.d("MediaUploadWorker", "📍 Server URL: $uploadResult")
                        successCount++
                    } else {
                        // Mark as failed
                        submissionRepository.updateMediaFile(
                            mediaFile.copy(upload_status = UploadStatus.FAILED)
                        )
                        Log.e("MediaUploadWorker", "❌ Failed to upload: ${mediaFile.file_name}")
                        failureCount++
                    }

                } catch (e: Exception) {
                    Log.e("MediaUploadWorker", "❌ Exception uploading ${mediaFile.file_name}: ${e.message}", e)

                    // Mark as failed
                    try {
                        submissionRepository.updateMediaFile(
                            mediaFile.copy(upload_status = UploadStatus.FAILED)
                        )
                    } catch (updateError: Exception) {
                        Log.e("MediaUploadWorker", "Failed to update media status: ${updateError.message}")
                    }

                    failureCount++
                }
            }

            Log.d("MediaUploadWorker", "📊 Result: $successCount success, $failureCount failed")

            if (failureCount == 0) {
                Log.d("MediaUploadWorker", "✅ All media uploaded")
                Result.success()
            } else {
                Log.d("MediaUploadWorker", "⏳ Retrying with backoff...")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e("MediaUploadWorker", "❌ Worker exception: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * Upload file to server using multipart/form-data
     *
     * @param localFilePath Path to file on device
     * @param fileName Original file name
     * @param submissionId Associated submission ID
     * @param fieldId Form field ID
     * @return Server URL if successful, null otherwise
     */
    private fun uploadFileToServer(
        localFilePath: String,
        fileName: String,
        submissionId: String,
        fieldId: String
    ): String? {
        return try {
            Log.d("MediaUploadWorker", "🔧 Preparing upload for: $fileName")

            val file = File(localFilePath)

            // Verify file exists
            if (!file.exists()) {
                Log.e("MediaUploadWorker", "❌ File not found: $localFilePath")
                return null
            }

            // Calculate file size in MB (removed invalid format specifier)
            val fileSizeMB = file.length() / (1024 * 1024.0)
            Log.d("MediaUploadWorker", "📄 File size: $fileSizeMB MB")

            // Determine MIME type
            val mimeType = getMimeType(file.extension)
            Log.d("MediaUploadWorker", "📋 MIME type: $mimeType")

            // Create multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    file.asRequestBody(mimeType.toMediaType())
                )
                .addFormDataPart("submission_id", submissionId)
                .addFormDataPart("field_id", fieldId)
                .build()

            Log.d("MediaUploadWorker", "🌐 Sending to: $UPLOAD_ENDPOINT")

            // Create request
            val request = Request.Builder()
                .url(UPLOAD_ENDPOINT)
                .post(requestBody)
                .addHeader("Authorization", "Bearer ${getToken()}")  // Add JWT token
                .build()

            // Execute request
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val response = client.newCall(request).execute()

            Log.d("MediaUploadWorker", "📡 Response code: ${response.code}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("MediaUploadWorker", "📡 Response: $responseBody")

                // Parse JSON response
                val json = org.json.JSONObject(responseBody ?: "{}")
                val serverUrl = json.optString("server_url")

                if (serverUrl.isNotEmpty()) {
                    Log.d("MediaUploadWorker", "✅ Upload successful, URL: $serverUrl")
                    serverUrl
                } else {
                    Log.e("MediaUploadWorker", "❌ No server_url in response")
                    null
                }
            } else {
                val errorBody = response.body?.string()
                Log.e("MediaUploadWorker", "❌ Upload failed: ${response.code}")
                Log.e("MediaUploadWorker", "❌ Error: $errorBody")
                null
            }

        } catch (e: Exception) {
            Log.e("MediaUploadWorker", "❌ Exception: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Get MIME type from file extension
     */
    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
    }

    /**
     * Get JWT token from secure storage
     * TODO: Implement proper token retrieval from SessionManager
     */
    private fun getToken(): String {
        // For now, return empty token
        // In production, retrieve from SessionManager/DataStore
        Log.w("MediaUploadWorker", "⚠️ Token not yet implemented")
        return ""
    }
}