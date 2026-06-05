package com.jsac.sync.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jsac.sync.data.local.db.entity.FormSubmissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FormSubmissionDao {

    // ============================================
    // WRITE OPERATIONS
    // ============================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: FormSubmissionEntity): Long

    @Update
    suspend fun updateSubmission(submission: FormSubmissionEntity)

    @Delete
    suspend fun deleteSubmission(submission: FormSubmissionEntity)

    // ============================================
    // READ OPERATIONS (FLOW - Reactive)
    // ============================================

    @Query("SELECT * FROM form_submissions WHERE id = :submissionId")
    fun getSubmissionById(submissionId: Int): Flow<FormSubmissionEntity?>

    @Query("SELECT * FROM form_submissions WHERE form_id = :formId ORDER BY created_at DESC")
    fun getSubmissionsByFormId(formId: String): Flow<List<FormSubmissionEntity>>

    @Query("SELECT * FROM form_submissions WHERE sync_status = :status ORDER BY created_at DESC")
    fun getSubmissionsByStatus(status: String): Flow<List<FormSubmissionEntity>>

    @Query("SELECT * FROM form_submissions ORDER BY created_at DESC")
    fun getAllSubmissions(): Flow<List<FormSubmissionEntity>>

    // ============================================
    // READ OPERATIONS (Suspend - One-time)
    // ============================================

    /**
     * Get single submission without Flow
     * Useful for repositories that need to call suspension functions
     */
    @Query("SELECT * FROM form_submissions WHERE id = :submissionId")
    suspend fun getSubmissionByIdOnce(submissionId: Int): FormSubmissionEntity?

    @Query("SELECT * FROM form_submissions WHERE sync_status = :status")
    suspend fun getSubmissionsByStatusOnce(status: String): List<FormSubmissionEntity>

    // ============================================
    // QUERIES FOR SYNC
    // ============================================

    @Query("SELECT * FROM form_submissions WHERE sync_status = 'PENDING' OR sync_status = 'FAILED' ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingSyncSubmissions(limit: Int = 50): List<FormSubmissionEntity>

    @Query("UPDATE form_submissions SET sync_status = :newStatus, updated_at = :timestamp WHERE id = :submissionId")
    suspend fun updateSubmissionStatus(
        submissionId: Int,
        newStatus: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE form_submissions SET sync_status = 'SYNCED', synced_at = :timestamp, updated_at = :timestamp WHERE id = :submissionId")
    suspend fun markAsSynced(
        submissionId: Int,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE form_submissions SET sync_status = 'FAILED', error_message = :errorMsg, retry_count = retry_count + 1, last_sync_attempt = :timestamp WHERE id = :submissionId")
    suspend fun markAsFailed(
        submissionId: Int,
        errorMsg: String,
        timestamp: Long = System.currentTimeMillis()
    )

    // ============================================
    // COUNT OPERATIONS
    // ============================================

    @Query("SELECT COUNT(*) FROM form_submissions WHERE form_id = :formId")
    fun getSubmissionCountForForm(formId: String): Flow<Int>

    /**
     * Count submissions by status
     * Used for showing badge counts in UI
     * @param status The sync status to count (e.g., "PENDING", "SYNCED", "FAILED")
     * @return Count of submissions with given status
     */
    @Query("SELECT COUNT(*) FROM form_submissions WHERE sync_status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM form_submissions WHERE sync_status = 'PENDING' OR sync_status = 'FAILED'")
    suspend fun countPendingSync(): Int

    // ============================================
    // DELETE OPERATIONS
    // ============================================

    @Query("DELETE FROM form_submissions WHERE id = :submissionId")
    suspend fun deleteSubmissionById(submissionId: Int)

    @Query("DELETE FROM form_submissions WHERE form_id = :formId")
    suspend fun deleteSubmissionsByFormId(formId: String)

    @Query("DELETE FROM form_submissions WHERE sync_status = 'SYNCED' AND synced_at < :beforeTime")
    suspend fun deleteSyncedBefore(beforeTime: Long)

    // ============================================
    // STATISTICS
    // ============================================

    @Query("SELECT sync_status, COUNT(*) as count FROM form_submissions GROUP BY sync_status")
    suspend fun getSyncStatusStats(): List<SyncStatusCount>
}

/**
 * Helper data class for sync status statistics
 */
data class SyncStatusCount(
    val sync_status: String,
    val count: Int
)