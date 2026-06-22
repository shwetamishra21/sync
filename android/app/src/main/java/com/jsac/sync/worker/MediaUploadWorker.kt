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
import com.jsac.sync.BuildConfig
import com.jsac.sync.data.local.datastore.SessionManager
import com.jsac.sync.data.local.db.entity.UploadStatus
import com.jsac.sync.data.repository.FormSubmissionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class MediaUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val submissionRepository: FormSubmissionRepository,
    private val sessionManager: SessionManager
) : Worker(context, params) {

    companion object {
        const val WORK_TAG = "media_upload"
        const val WORK_NAME = "media_upload_work"

        // ✅ FIXED: use the same base URL the rest of the app uses (debug/release aware)
        private val UPLOAD_ENDPOINT: String
            get() = "${BuildConfig.API_BASE_URL}media/upload"

        fun scheduleUpload(context: Context) {
            Log.d("MediaUploadWorker", "📅 Scheduling media upload...")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadRequest = OneTimeWorkRequestBuilder<MediaUploadWorker>()
                .addTag(WORK_TAG)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                androidx.work.ExistingWorkPolicy.KEEP,
                uploadRequest
            )

            Log.d("MediaUploadWorker", "✅ Upload scheduled")
        }

        fun cancelUpload(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }
    }

    override fun doWork(): Result = runBlocking {
        Log.d("MediaUploadWorker", "🎥 Starting media upload...")

        return@runBlocking try {
            // ✅ FIXED: get a real token once, like FormSyncWorker does
            val token = sessionManager.token.first()
            if (token.isNullOrEmpty()) {
                Log.e("MediaUploadWorker", "❌ No auth token — user not logged in, will retry later")
                return@runBlocking Result.retry()
            }

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
                    submissionRepository.updateMediaFile(
                        mediaFile.copy(upload_status = UploadStatus.UPLOADING)
                    )

                    val uploadResult = uploadFileToServer(
                        mediaFile.local_path,
                        mediaFile.file_name,
                        mediaFile.submission_id.toString(),
                        mediaFile.field_id,
                        token
                    )

                    if (uploadResult != null) {
                        submissionRepository.markMediaAsUploaded(mediaFile.id, uploadResult)
                        Log.d("MediaUploadWorker", "✅ Uploaded: ${mediaFile.file_name}")
                        successCount++
                    } else {
                        submissionRepository.updateMediaFile(
                            mediaFile.copy(upload_status = UploadStatus.FAILED)
                        )
                        failureCount++
                    }

                } catch (e: Exception) {
                    Log.e("MediaUploadWorker", "❌ Exception uploading ${mediaFile.file_name}: ${e.message}", e)
                    try {
                        submissionRepository.updateMediaFile(
                            mediaFile.copy(upload_status = UploadStatus.FAILED)
                        )
                    } catch (_: Exception) {}
                    failureCount++
                }
            }

            Log.d("MediaUploadWorker", "📊 Result: $successCount success, $failureCount failed")

            if (failureCount == 0) Result.success() else Result.retry()

        } catch (e: Exception) {
            Log.e("MediaUploadWorker", "❌ Worker exception: ${e.message}", e)
            Result.retry()
        }
    }

    private fun uploadFileToServer(
        localFilePath: String,
        fileName: String,
        submissionId: String,
        fieldId: String,
        token: String
    ): String? {
        return try {
            val file = File(localFilePath)
            if (!file.exists()) {
                Log.e("MediaUploadWorker", "❌ File not found: $localFilePath")
                return null
            }

            val mimeType = getMimeType(file.extension)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, file.asRequestBody(mimeType.toMediaType()))
                .addFormDataPart("submission_id", submissionId)
                .addFormDataPart("field_id", fieldId)
                .build()

            val request = Request.Builder()
                .url(UPLOAD_ENDPOINT)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $token")
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val json = org.json.JSONObject(responseBody ?: "{}")
                val serverUrl = json.optString("server_url")
                if (serverUrl.isNotEmpty()) serverUrl else null
            } else {
                Log.e("MediaUploadWorker", "❌ Upload failed: ${response.code} ${response.body?.string()}")
                null
            }

        } catch (e: Exception) {
            Log.e("MediaUploadWorker", "❌ Exception: ${e.message}", e)
            null
        }
    }

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
}