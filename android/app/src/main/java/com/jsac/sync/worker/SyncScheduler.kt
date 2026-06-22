package com.jsac.sync.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * ✅ OPTIMIZED: Single entry point for all sync operations
 *
 * Removes:
 * - syncForms(), uploadMedia(), syncSubmission(), syncMultipleSubmissions()
 *
 * Keeps:
 * - scheduleSync() - Syncs all pending items (auto-triggered)
 *
 * Benefits:
 * - No race conditions from multiple triggers
 * - Simpler to understand and maintain
 * - Consistent retry logic
 * - No duplicate code
 */
object SyncScheduler {

    /**
     * ✅ SINGLE ENTRY POINT: Schedule background sync for all pending items
     *
     * Called by:
     * - FormDetailViewModel after form submission
     * - HomeFragment when app opens
     * - User manually triggers sync
     *
     * This method:
     * 1. Triggers FormSyncWorker to sync pending submissions
     * 2. WorkManager handles retries + backoff automatically
     * 3. All pending items synced in one worker run
     *
     * @param context Android context
     */
    fun scheduleSync(context: Context) {
        Log.d("SyncScheduler", "📅 Scheduling background sync for pending submissions...")

        try {
            // ✅ Network constraint: Only sync when connected
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)  // Allow in battery saver
                .setRequiresDeviceIdle(false)     // Don't wait for idle
                .build()

            // ✅ Create work request with backoff
            val syncRequest = OneTimeWorkRequestBuilder<FormSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    5,
                    TimeUnit.MINUTES  // Start at 5 min, exponential growth
                )
                .setInitialDelay(1, TimeUnit.SECONDS)  // Quick start
                .addTag("form_sync")
                .build()

            // ✅ Enqueue with KEEP policy: Don't duplicate if already running
            WorkManager.getInstance(context).enqueueUniqueWork(
                "form_sync_work",
                androidx.work.ExistingWorkPolicy.KEEP,  // ← Keep existing, don't add duplicate
                syncRequest
            )

            Log.d("SyncScheduler", "✅ Sync scheduled successfully")
            Log.d("SyncScheduler", "   • Network required: Yes (CONNECTED)")
            Log.d("SyncScheduler", "   • Initial delay: 1 second")
            Log.d("SyncScheduler", "   • Backoff: Exponential (5 min → 16+ hours)")
            Log.d("SyncScheduler", "   • Duplicate policy: KEEP (prevents race conditions)")

        } catch (e: Exception) {
            Log.e("SyncScheduler", "❌ Error scheduling sync: ${e.message}", e)
        }
    }

    /**
     * Cancel any pending sync operations
     * Use sparingly - only if you want to pause sync
     */
    fun cancelSync(context: Context) {
        Log.d("SyncScheduler", "❌ Canceling all pending syncs...")
        try {
            WorkManager.getInstance(context).cancelAllWorkByTag("form_sync")
            Log.d("SyncScheduler", "✅ Sync cancelled")
        } catch (e: Exception) {
            Log.e("SyncScheduler", "Error canceling sync: ${e.message}")
        }
    }

    /**
     * Check if sync is currently running
     *
     * @return true if FormSyncWorker is active, false otherwise
     */
    fun isSyncing(context: Context): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosByTag("form_sync")
                .get()

            workInfos.any { it.state == androidx.work.WorkInfo.State.RUNNING }
        } catch (e: Exception) {
            Log.e("SyncScheduler", "Error checking sync status: ${e.message}")
            false
        }
    }

    /**
     * Get count of pending items (for UI display)
     *
     * This is utility function for status display only
     * Actual sync happens in FormSyncWorker
     */
    suspend fun getPendingCount(repository: com.jsac.sync.data.repository.FormSubmissionRepository): Int {
        return try {
            repository.countByStatus("PENDING")
        } catch (e: Exception) {
            Log.e("SyncScheduler", "Error getting pending count: ${e.message}")
            0
        }
    }
}

