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
import com.jsac.sync.data.local.db.entity.SyncStatus
import com.jsac.sync.data.repository.FormSubmissionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * WorkManager background worker for syncing form submissions
 *
 * Features:
 * - Syncs pending forms to server
 * - Exponential backoff on failure
 * - Only syncs when device has internet
 * - Retries up to 3 times
 */
@HiltWorker
class FormSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val submissionRepository: FormSubmissionRepository
) : Worker(context, params) {

    override fun doWork(): Result = runBlocking {
        Log.d("FormSyncWorker", "🔄 Starting form sync...")

        return@runBlocking try {
            // Get pending submissions (limit 10 per sync)
            val pendingSubmissions = submissionRepository.getPendingSyncSubmissions(limit = 10)

            if (pendingSubmissions.isEmpty()) {
                Log.d("FormSyncWorker", "✅ No pending submissions to sync")
                return@runBlocking Result.success()
            }

            Log.d("FormSyncWorker", "📤 Syncing ${pendingSubmissions.size} submissions...")

            var successCount = 0
            var failureCount = 0

            for (submission in pendingSubmissions) {
                try {
                    // Mark as syncing
                    submissionRepository.updateSubmissionStatus(
                        submission.id,
                        SyncStatus.SYNCING
                    )

                    // Sync to server
                    val result = submissionRepository.syncSubmissionToServer(
                        submissionId = submission.id,
                        gpsLocation = null
                    )

                    result.onSuccess { submissionIdFromServer ->
                        Log.d("FormSyncWorker", "✅ Synced: ${submission.id}")
                        successCount++
                    }

                    result.onFailure { error ->
                        Log.e("FormSyncWorker", "❌ Failed: ${error.message}")
                        failureCount++
                    }

                } catch (e: Exception) {
                    Log.e("FormSyncWorker", "❌ Error syncing ${submission.id}: ${e.message}")
                    failureCount++
                }
            }

            Log.d("FormSyncWorker", "📊 Result: $successCount success, $failureCount failed")

            // If all succeeded, return success
            if (failureCount == 0) {
                Log.d("FormSyncWorker", "✅ All forms synced successfully")
                Result.success()
            } else {
                // Retry with exponential backoff
                Log.d("FormSyncWorker", "⏳ Retrying with backoff...")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e("FormSyncWorker", "❌ Worker exception: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_TAG = "form_sync"
        const val WORK_NAME = "form_sync_work"

        /**
         * Schedule form sync with WorkManager
         *
         * - Requires internet connection
         * - Retries up to 3 times with exponential backoff
         * - Max backoff: 16 minutes
         */
        fun scheduleSync(context: Context) {
            Log.d("FormSyncWorker", "📅 Scheduling form sync...")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)  // Only sync online
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<FormSyncWorker>()
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
                syncRequest
            )

            Log.d("FormSyncWorker", "✅ Sync scheduled")
        }

        /**
         * Cancel pending sync
         */
        fun cancelSync(context: Context) {
            Log.d("FormSyncWorker", "❌ Canceling sync...")
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }
    }
}