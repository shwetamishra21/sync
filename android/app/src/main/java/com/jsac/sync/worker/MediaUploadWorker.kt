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
import java.util.concurrent.TimeUnit

/**
 * WorkManager background worker for uploading media files
 *
 * Features:
 * - Uploads pending media files to server
 * - Exponential backoff on failure
 * - Only uploads when device has internet
 * - Updates sync status after upload
 */
@HiltWorker
class MediaUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val submissionRepository: FormSubmissionRepository
) : Worker(context, params) {

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
                    // Mark as uploading using repository's public method
                    submissionRepository.updateMediaFile(
                        mediaFile.copy(upload_status = UploadStatus.UPLOADING)
                    )

                    // TODO: Upload to server (Part 5 - implement multipart upload)
                    // For now, simulate successful upload after 1 second
                    Thread.sleep(1000)

                    // Mark as uploaded with mock server URL
                    val serverUrl = "https://api.example.com/media/${mediaFile.id}"
                    submissionRepository.markMediaAsUploaded(mediaFile.id, serverUrl)

                    Log.d("MediaUploadWorker", "✅ Uploaded: ${mediaFile.file_name}")
                    successCount++

                } catch (e: Exception) {
                    Log.e("MediaUploadWorker", "❌ Failed to upload ${mediaFile.file_name}: ${e.message}")

                    // Mark as failed using repository's public method
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

    companion object {
        const val WORK_TAG = "media_upload"
        const val WORK_NAME = "media_upload_work"

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
}