package com.jsac.sync.data.repository

import android.util.Log
import com.google.gson.Gson
import com.jsac.sync.data.local.db.dao.FormSubmissionDao
import com.jsac.sync.data.local.db.dao.SyncQueueDao
import com.jsac.sync.data.local.db.entity.FormSubmissionEntity
import com.jsac.sync.data.local.db.entity.SyncQueueEntity
import com.jsac.sync.data.local.db.entity.SyncStatus
import com.jsac.sync.data.local.db.entity.OperationType
import com.jsac.sync.data.remote.api.SubmissionApi
import com.jsac.sync.data.remote.dto.FormSubmissionRequest
import com.jsac.sync.data.remote.dto.FormSubmissionModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * FormSubmissionRepository
 *
 * Implements OFFLINE-FIRST pattern:
 * 1. Save form submission to local database FIRST
 * 2. Return success immediately to user
 * 3. Sync to backend when online (background)
 * 4. If sync fails, retry with exponential backoff
 *
 * This ensures users can fill and submit forms even without internet
 */
class FormSubmissionRepository @Inject constructor(
    private val api: SubmissionApi,
    private val submissionDao: FormSubmissionDao,
    private val syncQueueDao: SyncQueueDao,
    private val gson: Gson
) {

    // ============================================
    // OFFLINE-FIRST: Save submission locally
    // ============================================

    /**
     * Submit a form - OFFLINE-FIRST approach
     *
     * Process:
     * 1. Validate form data
     * 2. Save to local database (PENDING status)
     * 3. Add to sync queue for background sync
     * 4. Return success immediately
     * 5. Sync happens in background via WorkManager
     */
    fun submitForm(
        formId: String,
        formData: Map<String, Any>,
        gpsLocation: Map<String, Double>? = null
    ): Flow<Result<FormSubmissionModel>> = flow {
        try {
            Log.d("FormSubmissionRepo", "💾 Saving form submission locally: $formId")

            // 1. Create entity for database
            val entity = FormSubmissionEntity(
                form_id = formId,
                form_data = gson.toJson(formData),
                sync_status = SyncStatus.PENDING,
                created_at = System.currentTimeMillis(),
                updated_at = System.currentTimeMillis()
            )

            // 2. Insert into database
            val submissionId = submissionDao.insertSubmission(entity).toInt()
            Log.d("FormSubmissionRepo", "✅ Form saved locally with ID: $submissionId")

            // 3. Add to sync queue for background sync
            val queueItem = SyncQueueEntity(
                submission_id = submissionId,
                operation_type = OperationType.CREATE,
                status = "PENDING",
                created_at = System.currentTimeMillis(),
                next_retry_time = System.currentTimeMillis()  // Sync immediately when online
            )
            syncQueueDao.insertQueueItem(queueItem)
            Log.d("FormSubmissionRepo", "📋 Added to sync queue: $submissionId")

            // 4. Return success immediately (offline-first!)
            val model = FormSubmissionModel(
                id = submissionId,
                formId = formId,
                formData = formData,
                syncStatus = SyncStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )

            emit(Result.success(model))
            Log.d("FormSubmissionRepo", "✨ Form submission complete (pending sync)")

        } catch (e: Exception) {
            Log.e("FormSubmissionRepo", "❌ Error submitting form: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    // ============================================
    // BACKGROUND SYNC: Upload to server
    // ============================================

    /**
     * Sync a single submission to backend
     * Called by WorkManager in background
     */
    suspend fun syncSubmissionToServer(submissionId: Int): Result<String> {
        return try {
            Log.d("FormSubmissionRepo", "🔄 Syncing submission: $submissionId")

            // 1. Get submission from local database
            val submission = submissionDao.getSubmissionById(submissionId)
                .let { flow ->
                    var result: FormSubmissionEntity? = null
                    flow.collect { result = it }
                    result
                }

            if (submission == null) {
                Log.e("FormSubmissionRepo", "❌ Submission not found: $submissionId")
                return Result.failure(Exception("Submission not found"))
            }

            // 2. Mark as SYNCING
            submissionDao.updateSubmissionStatus(submissionId, SyncStatus.SYNCING)

            // 3. Prepare request
            val formData = gson.fromJson(submission.form_data, Map::class.java)
            val request = FormSubmissionRequest(
                formId = submission.form_id,
                formData = formData as Map<String, Any>,
                submittedAt = submission.created_at
            )

            // 4. Send to backend
            val response = api.submitForm(request)

            if (response.isSuccessful && response.body() != null) {
                // 5. Mark as SYNCED
                submissionDao.markAsSynced(submissionId)

                // 6. Remove from sync queue
                val queueItems = syncQueueDao.getQueueItemsBySubmissionId(submissionId)
                    .let { flow ->
                        var result: List<SyncQueueEntity>? = null
                        flow.collect { result = it }
                        result
                    } ?: emptyList()

                queueItems.forEach { item ->
                    syncQueueDao.markAsSynced(item.id)
                }

                Log.d("FormSubmissionRepo", "✅ Submission synced successfully: $submissionId")
                Result.success(response.body()!!.submissionId)

            } else {
                // Sync failed - will retry
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                submissionDao.markAsFailed(submissionId, errorMsg)
                Log.e("FormSubmissionRepo", "❌ Sync failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }

        } catch (e: Exception) {
            Log.e("FormSubmissionRepo", "❌ Sync exception: ${e.message}", e)
            submissionDao.markAsFailed(submissionId, e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    // ============================================
    // READ OPERATIONS: Get submissions
    // ============================================

    /**
     * Get all submissions for a form
     */
    fun getSubmissionsByForm(formId: String): Flow<List<FormSubmissionModel>> = flow {
        submissionDao.getSubmissionsByFormId(formId).collect { entities ->
            val models = entities.map { entity ->
                FormSubmissionModel(
                    id = entity.id,
                    formId = entity.form_id,
                    formData = gson.fromJson(entity.form_data, Map::class.java) as Map<String, Any>,
                    syncStatus = entity.sync_status,
                    createdAt = entity.created_at,
                    syncedAt = entity.synced_at,
                    errorMessage = entity.error_message,
                    retryCount = entity.retry_count
                )
            }
            emit(models)
        }
    }

    /**
     * Get submissions by sync status (PENDING, SYNCED, FAILED)
     */
    fun getSubmissionsByStatus(status: String): Flow<List<FormSubmissionModel>> = flow {
        submissionDao.getSubmissionsByStatus(status).collect { entities ->
            val models = entities.map { entity ->
                FormSubmissionModel(
                    id = entity.id,
                    formId = entity.form_id,
                    formData = gson.fromJson(entity.form_data, Map::class.java) as Map<String, Any>,
                    syncStatus = entity.sync_status,
                    createdAt = entity.created_at,
                    syncedAt = entity.synced_at,
                    errorMessage = entity.error_message,
                    retryCount = entity.retry_count
                )
            }
            emit(models)
        }
    }

    /**
     * Get single submission
     */
    fun getSubmissionById(submissionId: Int): Flow<FormSubmissionModel?> = flow {
        submissionDao.getSubmissionById(submissionId).collect { entity ->
            val model = entity?.let {
                FormSubmissionModel(
                    id = it.id,
                    formId = it.form_id,
                    formData = gson.fromJson(it.form_data, Map::class.java) as Map<String, Any>,
                    syncStatus = it.sync_status,
                    createdAt = it.created_at,
                    syncedAt = it.synced_at,
                    errorMessage = it.error_message,
                    retryCount = it.retry_count
                )
            }
            emit(model)
        }
    }

    /**
     * Get count of pending submissions (not yet synced)
     */
    fun getPendingSubmissionCount(): Flow<Int> = flow {
        submissionDao.getSubmissionCountForForm("").collect { count ->
            // Count all submissions with PENDING or FAILED status
            val pending = submissionDao.countByStatus(SyncStatus.PENDING)
            val failed = submissionDao.countByStatus(SyncStatus.FAILED)
            emit(pending + failed)
        }
    }

    // ============================================
    // SYNC STATISTICS
    // ============================================

    /**
     * Get sync statistics
     */
    suspend fun getSyncStats(): SyncStats {
        val pending = submissionDao.countByStatus(SyncStatus.PENDING)
        val syncing = submissionDao.countByStatus(SyncStatus.SYNCING)
        val synced = submissionDao.countByStatus(SyncStatus.SYNCED)
        val failed = submissionDao.countByStatus(SyncStatus.FAILED)

        return SyncStats(
            pending = pending,
            syncing = syncing,
            synced = synced,
            failed = failed,
            total = pending + syncing + synced + failed
        )
    }

    /**
     * Manual retry for failed submissions
     */
    suspend fun retryFailedSubmissions(): Result<Int> {
        return try {
            val failed = submissionDao.getSubmissionsByStatusOnce(SyncStatus.FAILED)
            var successCount = 0

            for (submission in failed) {
                val result = syncSubmissionToServer(submission.id)
                if (result.isSuccess) {
                    successCount++
                }
            }

            Log.d("FormSubmissionRepo", "✅ Retried $successCount/${failed.size} submissions")
            Result.success(successCount)

        } catch (e: Exception) {
            Log.e("FormSubmissionRepo", "❌ Retry error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ============================================
    // CLEANUP
    // ============================================

    /**
     * Delete old synced submissions (keep last 30 days)
     */
    suspend fun cleanupOldSubmissions(daysOld: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000)
        submissionDao.deleteSyncedBefore(cutoffTime)
        Log.d("FormSubmissionRepo", "🗑️ Cleaned up submissions older than $daysOld days")
    }
}

/**
 * Data class for sync statistics
 */
data class SyncStats(
    val pending: Int = 0,
    val syncing: Int = 0,
    val synced: Int = 0,
    val failed: Int = 0,
    val total: Int = 0
) {
    val percentSynced: Float
        get() = if (total > 0) (synced.toFloat() / total) * 100 else 0f
}