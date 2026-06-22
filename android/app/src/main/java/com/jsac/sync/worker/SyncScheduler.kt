package com.jsac.sync.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Single entry point for all submission → server sync operations.
 *
 * - scheduleSync(context)            -> sync ALL pending submissions
 * - scheduleSyncSingle(context, id)  -> sync ONE specific submission
 *
 * Both go through FormSyncWorker. Room is the source of truth; the UI
 * observes Room Flow, so once the worker updates a row's sync_status,
 * every screen showing that submission updates automatically.
 */
object SyncScheduler {

    const val WORK_TAG = "form_sync"
    const val WORK_NAME_ALL = "form_sync_work_all"
    private const val KEY_SUBMISSION_ID = "submission_id"

    private fun baseConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(false)
        .setRequiresDeviceIdle(false)
        .build()

    /**
     * Sync all pending/failed submissions.
     */
    fun scheduleSync(context: Context) {
        Log.d("SyncScheduler", "📅 Scheduling sync for ALL pending submissions...")

        try {
            val syncRequest = OneTimeWorkRequestBuilder<FormSyncWorker>()
                .setConstraints(baseConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .setInitialDelay(1, TimeUnit.SECONDS)
                .addTag(WORK_TAG)
                .build()

            // ✅ REPLACE: Use unique name with timestamp to allow queuing
            val uniqueWorkName = "form_sync_batch_${System.currentTimeMillis() / 60000}"  // One per minute

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.KEEP,  // Only keeps if SAME minute (prevents rapid duplicates)
                syncRequest
            )

            Log.d("SyncScheduler", "✅ Full sync scheduled: $uniqueWorkName")

        } catch (e: Exception) {
            Log.e("SyncScheduler", "❌ Error scheduling sync: ${e.message}", e)
        }
    }

    /**
     * Sync a single submission immediately (used by "Sync Now" buttons).
     */
    fun scheduleSyncSingle(context: Context, submissionId: Int) {
        Log.d("SyncScheduler", "📅 Scheduling sync for submission #$submissionId...")

        try {
            val inputData = Data.Builder()
                .putInt(KEY_SUBMISSION_ID, submissionId)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<FormSyncWorker>()
                .setConstraints(baseConstraints())
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .addTag(WORK_TAG)
                .addTag("form_sync_single_$submissionId")
                .build()

            // Use a unique name per submission so multiple single-syncs can run
            // independently of each other and of the "sync all" job.
            WorkManager.getInstance(context).enqueueUniqueWork(
                "form_sync_single_$submissionId",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

            Log.d("SyncScheduler", "✅ Single sync scheduled for #$submissionId")
        } catch (e: Exception) {
            Log.e("SyncScheduler", "❌ Error scheduling single sync: ${e.message}", e)
        }
    }

    fun cancelSync(context: Context) {
        Log.d("SyncScheduler", "❌ Canceling all pending syncs...")
        try {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        } catch (e: Exception) {
            Log.e("SyncScheduler", "Error canceling sync: ${e.message}")
        }
    }

    fun isSyncing(context: Context): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosByTag(WORK_TAG)
                .get()

            workInfos.any {
                it.state == androidx.work.WorkInfo.State.RUNNING ||
                        it.state == androidx.work.WorkInfo.State.ENQUEUED
            }
        } catch (e: Exception) {
            Log.e("SyncScheduler", "Error checking sync status: ${e.message}")
            false
        }
    }
}