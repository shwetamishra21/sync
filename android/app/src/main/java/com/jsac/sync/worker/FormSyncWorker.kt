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
 * ✅ ENHANCED: Sync worker with improved error logging and diagnostics
 *
 * Key Improvements:
 * 1. Gets token ONCE at start (not per request)
 * 2. Comprehensive error logging with stack traces
 * 3. Marks submissions as SYNCED only on actual success
 * 4. Tracks retry attempts with detailed logging
 * 5. Returns retry on partial failure
 * 6. Logs exact error messages from API
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
            Log.d("FormSyncWorker", "")
            Log.d("FormSyncWorker", "🔐 Authenticating...")

            val token = try {
                sessionManager.token.first()
            } catch (e: Exception) {
                Log.e("FormSyncWorker", "❌ Failed to get token: ${e.message}")
                Log.e("FormSyncWorker", "   Exception type: ${e.javaClass.simpleName}")
                e.printStackTrace()
                return@runBlocking Result.retry()
            }

            if (token.isNullOrEmpty()) {
                Log.e("FormSyncWorker", "❌ Token is null/empty after retrieval")
                Log.e("FormSyncWorker", "   This usually means user is not logged in")
                Log.e("FormSyncWorker", "   Submission will remain PENDING")
                Log.e("FormSyncWorker", "⏳ Retrying later (user may login then)")
                return@runBlocking Result.retry()
            }

            Log.d("FormSyncWorker", "✅ Token obtained successfully")
            Log.d("FormSyncWorker", "   Token length: ${token.length} characters")
            Log.d("FormSyncWorker", "   Preview: ${token.take(20)}...${token.takeLast(10)}")

            // ✅ STEP 2: Get pending submissions
            Log.d("FormSyncWorker", "")
            Log.d("FormSyncWorker", "📦 Checking for pending submissions...")

            val pendingSubmissions = try {
                submissionRepository.getPendingSyncSubmissions(limit = 10)
            } catch (e: Exception) {
                Log.e("FormSyncWorker", "❌ Failed to get submissions: ${e.message}")
                e.printStackTrace()
                return@runBlocking Result.retry()
            }

            if (pendingSubmissions.isEmpty()) {
                Log.d("FormSyncWorker", "✅ No pending submissions to sync")
                Log.d("FormSyncWorker", "")
                Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")
                Log.d("FormSyncWorker", "✅ SYNC COMPLETE - Nothing to do")
                Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")
                Log.d("FormSyncWorker", "")
                return@runBlocking Result.success()
            }

            Log.d("FormSyncWorker", "📤 Found ${pendingSubmissions.size} submission(s) to sync")
            Log.d("FormSyncWorker", "🔄 ───────────────────────────────────────────────────")

            var successCount = 0
            var failureCount = 0
            val failedSubmissions = mutableListOf<Pair<Int, String>>()

            // ✅ STEP 3: Sync each submission
            for ((index, submission) in pendingSubmissions.withIndex()) {
                try {
                    Log.d("FormSyncWorker", "")
                    Log.d("FormSyncWorker", "📋 [${index + 1}/${pendingSubmissions.size}] Syncing submission #${submission.id}")
                    Log.d("FormSyncWorker", "   Form: ${submission.form_id}")
                    Log.d("FormSyncWorker", "   Current status: ${submission.sync_status}")
                    Log.d("FormSyncWorker", "   Created: ${submission.created_at}")

                    // Mark as SYNCING
                    try {
                        submissionRepository.updateSubmissionStatus(
                            submission.id,
                            SyncStatus.SYNCING
                        )
                        Log.d("FormSyncWorker", "   ✅ Status updated to SYNCING")
                    } catch (e: Exception) {
                        Log.e("FormSyncWorker", "   ⚠️ Failed to mark as SYNCING: ${e.message}")
                    }

                    // Call API with token
                    val result = syncToServer(submission, token)

                    if (result) {
                        // Mark as SYNCED
                        try {
                            submissionRepository.markAsSynced(submission.id)
                            Log.d("FormSyncWorker", "   ✅ SYNCED successfully")
                            Log.d("FormSyncWorker", "   📊 Status in DB: SYNCED")
                            successCount++
                        } catch (e: Exception) {
                            Log.e("FormSyncWorker", "   ⚠️ Failed to mark as SYNCED: ${e.message}")
                        }
                    } else {
                        // Mark as FAILED (will retry next time)
                        try {
                            submissionRepository.markAsFailed(
                                submission.id,
                                "Sync returned false - check logs for details"
                            )
                            Log.e("FormSyncWorker", "   ❌ FAILED - will retry later")
                            Log.e("FormSyncWorker", "   📊 Status in DB: FAILED")
                            failureCount++
                            failedSubmissions.add(Pair(submission.id, "Sync returned false"))
                        } catch (e: Exception) {
                            Log.e("FormSyncWorker", "   ⚠️ Failed to mark as FAILED: ${e.message}")
                        }
                    }

                } catch (e: Exception) {
                    Log.e("FormSyncWorker", "   ❌ Exception during sync: ${e.message}")
                    Log.e("FormSyncWorker", "   Type: ${e.javaClass.simpleName}")
                    e.printStackTrace()

                    try {
                        submissionRepository.markAsFailed(
                            submission.id,
                            "Exception: ${e.message}"
                        )
                    } catch (updateError: Exception) {
                        Log.e("FormSyncWorker", "   Error updating status: ${updateError.message}")
                    }

                    failureCount++
                    failedSubmissions.add(Pair(submission.id, e.message ?: "Unknown error"))
                }
            }

            // ✅ STEP 4: Log results and decide retry
            Log.d("FormSyncWorker", "")
            Log.d("FormSyncWorker", "🔄 ───────────────────────────────────────────────────")
            Log.d("FormSyncWorker", "📊 SYNC SUMMARY")
            Log.d("FormSyncWorker", "   ✅ Successful: $successCount")
            Log.d("FormSyncWorker", "   ❌ Failed: $failureCount")
            Log.d("FormSyncWorker", "   📈 Success rate: ${if (pendingSubmissions.isNotEmpty()) (successCount * 100) / pendingSubmissions.size else 0}%")

            if (failedSubmissions.isNotEmpty()) {
                Log.d("FormSyncWorker", "   Failed submissions:")
                failedSubmissions.forEach { (id, reason) ->
                    Log.d("FormSyncWorker", "      • #$id: $reason")
                }
            }

            Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")

            // Return result
            when {
                failureCount == 0 -> {
                    Log.d("FormSyncWorker", "✅ ALL SUBMISSIONS SYNCED - Work complete")
                    Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")
                    Log.d("FormSyncWorker", "")
                    Result.success()
                }
                successCount > 0 && failureCount > 0 -> {
                    Log.w("FormSyncWorker", "⏳ PARTIAL SUCCESS - Retrying failed submissions")
                    Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")
                    Log.d("FormSyncWorker", "")
                    Result.retry()
                }
                else -> {
                    Log.w("FormSyncWorker", "⏳ ALL FAILED - Will retry with exponential backoff")
                    Log.d("FormSyncWorker", "   Next attempt in: 5-10 minutes")
                    Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")
                    Log.d("FormSyncWorker", "")
                    Result.retry()
                }
            }

        } catch (e: Exception) {
            Log.e("FormSyncWorker", "❌ WORKER EXCEPTION: ${e.message}")
            Log.e("FormSyncWorker", "   Type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Log.e("FormSyncWorker", "⏳ Scheduling retry with exponential backoff")
            Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")
            Log.d("FormSyncWorker", "")
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
            Log.d("FormSyncWorker", "   🌐 Calling repository sync method...")

            val result = submissionRepository.syncSubmissionToServer(
                submission.id,
                token = token,
                gpsLocation = null
            )

            result.onSuccess { serverId ->
                Log.d("FormSyncWorker", "   🌐 API call returned SUCCESS")
                Log.d("FormSyncWorker", "      Server submission ID: $serverId")
            }

            result.onFailure { error ->
                Log.e("FormSyncWorker", "   🌐 API call FAILED: ${error.message}")
                Log.e("FormSyncWorker", "      Type: ${error.javaClass.simpleName}")
                error.printStackTrace()
            }

            result.isSuccess

        } catch (e: Exception) {
            Log.e("FormSyncWorker", "   🌐 API call exception: ${e.message}")
            Log.e("FormSyncWorker", "      Type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            false
        }
    }

    companion object {
        const val WORK_TAG = "form_sync"
        const val WORK_NAME = "form_sync_work"

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

            Log.d("FormSyncWorker", "✅ Worker scheduled successfully")
        }
    }
}