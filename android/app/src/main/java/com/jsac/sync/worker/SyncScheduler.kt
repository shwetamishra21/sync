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
}