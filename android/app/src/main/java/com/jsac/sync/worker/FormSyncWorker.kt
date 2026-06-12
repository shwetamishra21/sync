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
import com.jsac.sync.data.local.datastore.SessionManager
import com.jsac.sync.data.local.db.entity.SyncStatus
import com.jsac.sync.data.repository.FormSubmissionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * ✅ OPTIMIZED: Single sync worker for all pending form submissions
 *
 * Key Improvements:
 * 1. Gets token ONCE at start (not per request)
 * 2. Proper error handling and logging
 * 3. Marks submissions as SYNCED only on actual success
 * 4. Tracks retry attempts with detailed logging
 * 5. Returns retry on partial failure (some succeeded, some failed)
 * 6. Clear separation of concerns
 *
 * Flow:
 * 1. Get pending submissions from Room (status = PENDING or SYNCING)
 * 2. For each submission:
 *    a. Mark as SYNCING
 *    b. Get token from SessionManager
 *    c. Call API: POST /forms/submit with JWT
 *    d. On success: Mark as SYNCED in Room
 *    e. On failure: Mark as FAILED, keep for retry
 * 3. Return success if all synced, retry if any failed
 * 4. WorkManager handles exponential backoff automatically
 */
@HiltWorker
class FormSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val submissionRepository: FormSubmissionRepository,
    private val sessionManager: SessionManager
) : Worker(context, params) {

    override fun doWork(): Result = runBlocking {
        Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")
        Log.d("FormSyncWorker", "🔄 Starting form sync worker")
        Log.d("FormSyncWorker", "   ID: $id")
        Log.d("FormSyncWorker", "   Attempt: ${runAttemptCount + 1}")
        Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")

        return@runBlocking try {
            // ✅ STEP 1: Get token once (not per request)
            val token = try {
                sessionManager.token.first()
            } catch (e: Exception) {
                Log.e("FormSyncWorker", "❌ Failed to get token: ${e.message}")
                return@runBlocking Result.retry()
            }

            if (token.isNullOrEmpty()) {
                Log.e("FormSyncWorker", "❌ Token is empty - user may not be logged in")
                Log.e("FormSyncWorker", "⏳ Retrying later (maybe user logs in)")
                return@runBlocking Result.retry()
            }

            Log.d("FormSyncWorker", "✅ Token obtained (length: ${token.length})")

            // ✅ STEP 2: Get pending submissions
            val pendingSubmissions = submissionRepository.getPendingSyncSubmissions(limit = 10)

            if (pendingSubmissions.isEmpty()) {
                Log.d("FormSyncWorker", "✅ No pending submissions to sync")
                Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")
                return@runBlocking Result.success()
            }

            Log.d("FormSyncWorker", "📤 Found ${pendingSubmissions.size} submission(s) to sync")
            Log.d("FormSyncWorker", "🔄 ───────────────────────────────────────────────────")

            var successCount = 0
            var failureCount = 0
            val failedSubmissions = mutableListOf<String>()

            // ✅ STEP 3: Sync each submission
            for (submission in pendingSubmissions) {
                try {
                    Log.d("FormSyncWorker", "")
                    Log.d("FormSyncWorker", "📋 Syncing submission #${submission.id}")
                    Log.d("FormSyncWorker", "   Form: ${submission.form_id}")
                    Log.d("FormSyncWorker", "   Current status: ${submission.sync_status}")

                    // Mark as SYNCING
                    submissionRepository.updateSubmissionStatus(
                        submission.id,
                        SyncStatus.SYNCING
                    )
                    Log.d("FormSyncWorker", "   → Marked as SYNCING")

                    // Call API with token
                    val result = syncToServer(submission, token)

                    if (result) {
                        // Mark as SYNCED
                        submissionRepository.markAsSynced(submission.id)
                        Log.d("FormSyncWorker", "   ✅ SYNCED successfully")
                        successCount++
                    } else {
                        // Mark as FAILED (will retry next time)
                        submissionRepository.markAsFailed(
                            submission.id,
                            "Network error or server rejected"
                        )
                        Log.e("FormSyncWorker", "   ❌ FAILED - will retry later")
                        failureCount++
                        failedSubmissions.add("#${submission.id}")
                    }

                } catch (e: Exception) {
                    Log.e("FormSyncWorker", "   ❌ Exception: ${e.message}", e)
                    try {
                        submissionRepository.markAsFailed(
                            submission.id,
                            e.message ?: "Unknown error"
                        )
                    } catch (updateError: Exception) {
                        Log.e("FormSyncWorker", "   Error updating status: ${updateError.message}")
                    }
                    failureCount++
                    failedSubmissions.add("#${submission.id}")
                }
            }

            // ✅ STEP 4: Log results and decide retry
            Log.d("FormSyncWorker", "")
            Log.d("FormSyncWorker", "🔄 ───────────────────────────────────────────────────")
            Log.d("FormSyncWorker", "📊 SYNC SUMMARY")
            Log.d("FormSyncWorker", "   ✅ Successful: $successCount")
            Log.d("FormSyncWorker", "   ❌ Failed: $failureCount")

            if (failedSubmissions.isNotEmpty()) {
                Log.d("FormSyncWorker", "   Failed IDs: ${failedSubmissions.joinToString(", ")}")
            }

            Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")

            // Return result
            when {
                failureCount == 0 -> {
                    Log.d("FormSyncWorker", "✅ All submissions synced! Sync complete.")
                    Result.success()
                }
                successCount > 0 && failureCount > 0 -> {
                    Log.w("FormSyncWorker", "⏳ Partial success - retrying failed submissions")
                    Result.retry()
                }
                else -> {
                    Log.w("FormSyncWorker", "⏳ All failed - will retry with exponential backoff")
                    Result.retry()
                }
            }

        } catch (e: Exception) {
            Log.e("FormSyncWorker", "❌ Worker exception: ${e.message}", e)
            Log.e("FormSyncWorker", "⏳ Scheduling retry with exponential backoff")
            Result.retry()
        }
    }

    /**
     * Sync single submission to server
     * Returns true if successful, false if failed
     */
    private suspend fun syncToServer(
        submission: com.jsac.sync.data.local.db.entity.FormSubmissionEntity,
        token: String
    ): Boolean {
        return try {
            Log.d("FormSyncWorker", "   🌐 Calling API: POST /forms/submit")

            val result = submissionRepository.syncSubmissionToServer(
                submission.id,
                token = token,
                gpsLocation = null
            )

            result.isSuccess

        } catch (e: Exception) {
            Log.e("FormSyncWorker", "   🌐 API call failed: ${e.message}")
            false
        }
    }

    companion object {
        const val WORK_TAG = "form_sync"
        const val WORK_NAME = "form_sync_work"

        /**
         * Schedule worker (called by SyncScheduler)
         *
         * Note: SyncScheduler.scheduleSync() should be the ONLY entry point
         * This function is internal, not called elsewhere
         */
        fun schedule(context: Context) {
            Log.d("FormSyncWorker", "📅 Scheduling FormSyncWorker...")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .setRequiresDeviceIdle(false)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<FormSyncWorker>()
                .addTag(WORK_TAG)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    5,
                    TimeUnit.MINUTES
                )
                .setInitialDelay(1, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                androidx.work.ExistingWorkPolicy.KEEP,
                syncRequest
            )

            Log.d("FormSyncWorker", "✅ Worker scheduled")
        }
    }
}

/**
 * 🗑️ REMOVED from old implementation:
 *
 * ❌ canCancel() - Rarely needed
 * ❌ isSyncScheduled() - Use SyncScheduler.isSyncing()
 * ❌ Multiple logging levels - Simplified to be clear
 * ❌ syncSubmissionToServer() is now syncWithToken() internally
 *
 * 🔑 KEY CHANGE:
 * Old: Token fetched per request (with runBlocking in interceptor)
 * New: Token fetched once at worker start, passed to API calls
 *
 * This prevents:
 * - Potential deadlocks from nested runBlocking calls
 * - Network requests without token
 * - Race conditions in token retrieval
 */