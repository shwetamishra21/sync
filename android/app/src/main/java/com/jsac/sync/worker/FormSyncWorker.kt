package com.jsac.sync.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.jsac.sync.data.local.datastore.SessionManager
import com.jsac.sync.data.local.db.entity.FormSubmissionEntity
import com.jsac.sync.data.local.db.entity.SyncStatus
import com.jsac.sync.data.repository.FormSubmissionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * ✅ ENHANCED: Sync worker with improved error logging and diagnostics
 * ✅ FIX #6: Implements maximum retry attempt limits
 *
 * Can run in two modes (controlled by SyncScheduler):
 * - "sync all pending"      -> no submission_id in inputData
 * - "sync one submission"   -> submission_id present in inputData
 *
 * After MAX_SYNC_ATTEMPTS is exceeded, submissions are marked as FAILED
 * and user must manually retry from submission list.
 */
@HiltWorker
class FormSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val submissionRepository: FormSubmissionRepository,
    private val sessionManager: SessionManager
) : Worker(context, params) {

    override fun doWork(): Result = runBlocking {
        // ✅ Read this inside doWork(), not as a class property —
        // inputData is only guaranteed safe to read once work starts.
        val targetSubmissionId = inputData.getInt("submission_id", -1)
        val maxAttempts = inputData.getInt("max_attempts", 8)  // ✅ NEW: Get max attempts from input

        Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")
        Log.d("FormSyncWorker", "🔄 Starting form sync worker")
        Log.d("FormSyncWorker", "   ID: $id")
        Log.d("FormSyncWorker", "   Attempt: ${runAttemptCount + 1}/$maxAttempts")  // ✅ Show attempt count
        Log.d("FormSyncWorker", "🔄 ═══════════════════════════════════════════════════")

        // ✅ FIX #6: CHECK ATTEMPT LIMIT BEFORE PROCEEDING
        if (runAttemptCount >= maxAttempts) {
            Log.e("FormSyncWorker", "")
            Log.e("FormSyncWorker", "❌ ═══════════════════════════════════════════════════")
            Log.e("FormSyncWorker", "❌ MAX ATTEMPTS EXCEEDED ($maxAttempts)")
            Log.e("FormSyncWorker", "❌ Marking all pending submissions as FAILED")
            Log.e("FormSyncWorker", "❌ User must manually retry from submission list")
            Log.e("FormSyncWorker", "❌ ═══════════════════════════════════════════════════")

            return@runBlocking try {
                // Get all pending submissions and mark as failed
                val pendingSubmissions = submissionRepository.getPendingSyncSubmissions(limit = 1000)
                Log.d("FormSyncWorker", "Found ${pendingSubmissions.size} pending submissions to mark as failed")

                for (submission in pendingSubmissions) {
                    try {
                        submissionRepository.markAsFailed(
                            submission.id,
                            "Exceeded maximum sync attempts ($maxAttempts). Server may be down. Please retry manually."
                        )
                        Log.d("FormSyncWorker", "  ✅ Marked submission #${submission.id} as FAILED")
                    } catch (e: Exception) {
                        Log.e("FormSyncWorker", "  ❌ Error marking submission #${submission.id} as failed: ${e.message}")
                    }
                }

                Log.d("FormSyncWorker", "✅ All pending submissions marked as FAILED. Stopping retries.")
                Result.success()  // ← GIVE UP, STOP RETRYING
            } catch (e: Exception) {
                Log.e("FormSyncWorker", "❌ Error during max attempts handling: ${e.message}")
                e.printStackTrace()
                Result.success()  // ← Still stop retrying
            }
        }

        return@runBlocking try {
            // ✅ STEP 1: Get token once (not per request)
            Log.d("FormSyncWorker", "")
            Log.d("FormSyncWorker", "🔐 Authenticating...")

            val token = try {
                sessionManager.token.first()
            } catch (e: Exception) {
                Log.e("FormSyncWorker", "❌ Failed to get token: ${e.message}")
                e.printStackTrace()
                return@runBlocking Result.retry()
            }

            if (token.isNullOrEmpty()) {
                Log.e("FormSyncWorker", "❌ Token is null/empty after retrieval")
                Log.e("FormSyncWorker", "   This usually means user is not logged in")
                return@runBlocking Result.retry()
            }

            Log.d("FormSyncWorker", "✅ Token obtained successfully (length: ${token.length})")

            // ✅ STEP 2: Get submissions to sync (single target or all pending)
            Log.d("FormSyncWorker", "")
            Log.d("FormSyncWorker", "📦 Checking for submissions to sync...")

            val pendingSubmissions = try {
                if (targetSubmissionId != -1) {
                    Log.d("FormSyncWorker", "🎯 Single-submission sync requested: #$targetSubmissionId")
                    val single = submissionRepository.getSubmissionByIdOnce(targetSubmissionId)
                    if (single != null) listOf(single) else emptyList()
                } else {
                    submissionRepository.getPendingSyncSubmissions(limit = 10)
                }
            } catch (e: Exception) {
                Log.e("FormSyncWorker", "❌ Failed to get submissions: ${e.message}")
                e.printStackTrace()
                return@runBlocking Result.retry()
            }

            if (pendingSubmissions.isEmpty()) {
                Log.d("FormSyncWorker", "✅ No submissions to sync")
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

                    try {
                        submissionRepository.updateSubmissionStatus(submission.id, SyncStatus.SYNCING)
                        Log.d("FormSyncWorker", "   ✅ Status updated to SYNCING")
                    } catch (e: Exception) {
                        Log.e("FormSyncWorker", "   ❌ CRITICAL: Failed to mark as SYNCING")

                        // ✅ ABORT THIS SUBMISSION, TRY AGAIN LATER
                        failureCount++
                        failedSubmissions.add(Pair(submission.id, "Cannot update status: ${e.message}"))

                        // Log critical error
                        try {
                            submissionRepository.markAsFailed(
                                submission.id,
                                "Worker failed to update status to SYNCING: ${e.message}"
                            )
                        } catch (updateError: Exception) {
                            Log.e("FormSyncWorker", "   ❌ Also failed to mark as FAILED: ${updateError.message}")
                        }

                        continue  // ← SKIP THIS SUBMISSION, DON'T CALL API
                    }

                    val result = syncToServer(submission, token)

                    if (result) {
                        try {
                            submissionRepository.markAsSynced(submission.id)
                            Log.d("FormSyncWorker", "   ✅ SYNCED successfully")
                            successCount++
                        } catch (e: Exception) {
                            Log.e("FormSyncWorker", "   ⚠️ Failed to mark as SYNCED: ${e.message}")
                        }
                    } else {
                        try {
                            submissionRepository.markAsFailed(
                                submission.id,
                                "Sync returned false - check logs for details"
                            )
                            Log.e("FormSyncWorker", "   ❌ FAILED - will retry later")
                            failureCount++
                            failedSubmissions.add(Pair(submission.id, "Sync returned false"))
                        } catch (e: Exception) {
                            Log.e("FormSyncWorker", "   ⚠️ Failed to mark as FAILED: ${e.message}")
                        }
                    }

                } catch (e: Exception) {
                    Log.e("FormSyncWorker", "   ❌ Exception during sync: ${e.message}")
                    e.printStackTrace()

                    try {
                        submissionRepository.markAsFailed(submission.id, "Exception: ${e.message}")
                    } catch (updateError: Exception) {
                        Log.e("FormSyncWorker", "   Error updating status: ${updateError.message}")
                    }

                    failureCount++
                    failedSubmissions.add(Pair(submission.id, e.message ?: "Unknown error"))
                }
            }

            // ✅ STEP 4: Log results and decide retry
            Log.d("FormSyncWorker", "")
            Log.d("FormSyncWorker", "📊 SYNC SUMMARY — ✅ $successCount  ❌ $failureCount")

            if (failedSubmissions.isNotEmpty()) {
                failedSubmissions.forEach { (submissionId, reason) ->
                    Log.d("FormSyncWorker", "      • #$submissionId: $reason")
                }
            }

            when {
                failureCount == 0 -> {
                    Log.d("FormSyncWorker", "✅ ALL SUBMISSIONS SYNCED - Work complete")
                    Result.success()
                }
                successCount > 0 -> {
                    Log.w("FormSyncWorker", "⏳ PARTIAL SUCCESS - Retrying failed submissions")
                    Result.retry()
                }
                else -> {
                    Log.w("FormSyncWorker", "⏳ ALL FAILED - Will retry with exponential backoff (attempt ${runAttemptCount + 1}/$maxAttempts)")
                    Result.retry()
                }
            }

        } catch (e: Exception) {
            Log.e("FormSyncWorker", "❌ WORKER EXCEPTION: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }

    /**
     * Sync single submission to server. Returns true if successful.
     */
    private suspend fun syncToServer(
        submission: FormSubmissionEntity,
        token: String
    ): Boolean {
        return try {
            Log.d("FormSyncWorker", "   🌐 Syncing submission #${submission.id} to server...")

            val result = submissionRepository.syncSubmissionToServer(
                submissionId = submission.id,
                token = token,
                gpsLocation = null
            )

            result.onSuccess { serverId ->
                Log.d("FormSyncWorker", "   ✅ API call SUCCESS — server ID: $serverId")
            }
            result.onFailure { error ->
                Log.e("FormSyncWorker", "   ❌ API call FAILED: ${error.message}")
                error.printStackTrace()
            }

            result.isSuccess

        } catch (e: Exception) {
            Log.e("FormSyncWorker", "   ❌ API call exception: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}