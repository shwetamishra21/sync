package com.jsac.sync.worker

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.jsac.sync.data.repository.FormSubmissionRepository

/**
 * Helper class to manage background sync tasks
 *
 * Provides simple interface to:
 * - Trigger form sync
 * - Trigger media upload
 * - Check sync status
 * - Cancel sync tasks
 */
object SyncScheduler {

    /**
     * Trigger immediate form sync
     * Called when:
     * - User submits a form
     * - User opens app (if has pending forms)
     * - App detects network connection
     */
    fun syncForms(context: Context) {
        Log.d("SyncScheduler", "📋 Triggering form sync...")
        FormSyncWorker.scheduleSync(context)
    }

    /**
     * Trigger immediate media upload
     * Called when:
     * - User adds photos/documents
     * - User opens app (if has pending media)
     * - App detects network connection
     */
    fun uploadMedia(context: Context) {
        Log.d("SyncScheduler", "📸 Triggering media upload...")
        MediaUploadWorker.scheduleUpload(context)
    }

    /**
     * Sync everything (forms + media)
     * Called by HomeFragment when user sees app
     */
    fun syncAll(context: Context) {
        Log.d("SyncScheduler", "🔄 Triggering full sync...")
        syncForms(context)
        uploadMedia(context)
    }

    /**
     * Cancel all sync tasks
     */
    fun cancelAll(context: Context) {
        Log.d("SyncScheduler", "❌ Canceling all sync tasks...")
        FormSyncWorker.cancelSync(context)
        MediaUploadWorker.cancelUpload(context)
    }

    /**
     * Cancel form sync only
     */
    fun cancelFormSync(context: Context) {
        Log.d("SyncScheduler", "❌ Canceling form sync...")
        FormSyncWorker.cancelSync(context)
    }

    /**
     * Cancel media upload only
     */
    fun cancelMediaUpload(context: Context) {
        Log.d("SyncScheduler", "❌ Canceling media upload...")
        MediaUploadWorker.cancelUpload(context)
    }

    /**
     * Check if sync is in progress
     */
    fun isSyncing(context: Context): Boolean {
        val workManager = WorkManager.getInstance(context)
        val formSyncWorks = workManager.getWorkInfosByTag(FormSyncWorker.WORK_TAG)
        val mediaUploadWorks = workManager.getWorkInfosByTag(MediaUploadWorker.WORK_TAG)

        return try {
            // Get work status (blocking call - OK for UI status checks)
            val formSyncing = formSyncWorks.get().any { it.state.isFinished.not() }
            val mediaUploading = mediaUploadWorks.get().any { it.state.isFinished.not() }

            formSyncing || mediaUploading
        } catch (e: Exception) {
            Log.e("SyncScheduler", "Error checking sync status: ${e.message}")
            false
        }
    }

    /**
     * Get pending form count
     * Useful for showing badge on dashboard
     */
    suspend fun getPendingFormCount(repository: FormSubmissionRepository): Int {
        return repository.countByStatus("PENDING")
    }

    /**
     * Get pending media count
     * Useful for showing upload progress
     */
    suspend fun getPendingMediaCount(repository: FormSubmissionRepository): Int {
        return repository.countByStatus("LOCAL")
    }
    // ============================================
// ADD THESE METHODS TO: SyncScheduler.kt
// Location: android/app/src/main/java/com/jsac/sync/worker/SyncScheduler.kt
// ============================================

    /**
     * Sync a specific submission
     * Called when user manually triggers sync from UI
     *
     * @param context Android context
     * @param submissionId ID of submission to sync
     */
    fun syncSubmission(context: Context, submissionId: Int) {
        Log.d("SyncScheduler", "📋 Triggering sync for submission: $submissionId")

        try {
            // Create data that will be passed to the worker
            val inputData = androidx.work.Data.Builder()
                .putInt("submission_id", submissionId)
                .build()

            // Create one-time work request for this submission
            val syncRequest = androidx.work.OneTimeWorkRequestBuilder<FormSyncWorker>()
                .setInputData(inputData)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    5,
                    java.util.concurrent.TimeUnit.MINUTES
                )
                .addTag("submission_sync_$submissionId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sync_submission_$submissionId",
                androidx.work.ExistingWorkPolicy.KEEP,
                syncRequest
            )

            Log.d("SyncScheduler", "✅ Submission sync scheduled for ID: $submissionId")

        } catch (e: Exception) {
            Log.e("SyncScheduler", "❌ Error scheduling sync: ${e.message}", e)
            throw e
        }
    }

    /**
     * Sync multiple submissions
     * Called when user selects multiple submissions and syncs them
     *
     * @param context Android context
     * @param submissionIds List of submission IDs to sync
     */
    fun syncMultipleSubmissions(context: Context, submissionIds: List<Int>) {
        Log.d("SyncScheduler", "📋 Triggering sync for ${submissionIds.size} submissions")

        if (submissionIds.isEmpty()) {
            Log.w("SyncScheduler", "⚠️ No submissions to sync")
            return
        }

        try {
            // Create data with submission IDs as a JSON array
            val idsJson = com.google.gson.Gson().toJson(submissionIds)
            val inputData = androidx.work.Data.Builder()
                .putString("submission_ids", idsJson)
                .build()

            // Create one-time work request for these submissions
            val syncRequest = androidx.work.OneTimeWorkRequestBuilder<FormSyncWorker>()
                .setInputData(inputData)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    5,
                    java.util.concurrent.TimeUnit.MINUTES
                )
                .addTag("batch_sync_${System.currentTimeMillis()}")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sync_batch_${System.currentTimeMillis()}",
                androidx.work.ExistingWorkPolicy.REPLACE,
                syncRequest
            )

            Log.d("SyncScheduler", "✅ Batch sync scheduled for ${submissionIds.size} submissions")

        } catch (e: Exception) {
            Log.e("SyncScheduler", "❌ Error scheduling batch sync: ${e.message}", e)
            throw e
        }
    }

    /**
     * Get sync status for a specific submission
     * Returns a Flow that emits the current sync status
     *
     * @param context Android context
     * @param submissionId ID of submission
     * @return Flow of work info for the submission
     */
    fun getSyncStatus(context: Context, submissionId: Int) =
        WorkManager.getInstance(context)
            .getWorkInfosByTagLiveData("submission_sync_$submissionId")

    /**
     * Cancel sync for a specific submission
     *
     * @param context Android context
     * @param submissionId ID of submission
     */
    fun cancelSync(context: Context, submissionId: Int) {
        Log.d("SyncScheduler", "❌ Canceling sync for submission: $submissionId")

        try {
            WorkManager.getInstance(context)
                .cancelAllWorkByTag("submission_sync_$submissionId")

            Log.d("SyncScheduler", "✅ Sync cancelled for submission: $submissionId")

        } catch (e: Exception) {
            Log.e("SyncScheduler", "❌ Error canceling sync: ${e.message}", e)
        }
    }

    /**
     * Check if a submission is currently syncing
     *
     * @param context Android context
     * @param submissionId ID of submission
     * @return true if currently syncing, false otherwise
     */
    fun isSubmissionSyncing(context: Context, submissionId: Int): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosByTag("submission_sync_$submissionId")
                .get()

            workInfos.any { it.state == androidx.work.WorkInfo.State.RUNNING }

        } catch (e: Exception) {
            Log.e("SyncScheduler", "Error checking sync status: ${e.message}")
            false
        }
    }
}