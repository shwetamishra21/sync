package com.jsac.sync.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jsac.sync.data.local.db.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    // ============================================
    // WRITE OPERATIONS
    // ============================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: SyncQueueEntity): Long

    @Update
    suspend fun updateQueueItem(item: SyncQueueEntity)

    @Delete
    suspend fun deleteQueueItem(item: SyncQueueEntity)

    // ============================================
    // READ OPERATIONS
    // ============================================

    @Query("SELECT * FROM sync_queue WHERE id = :queueId")
    fun getQueueItemById(queueId: Int): Flow<SyncQueueEntity?>

    @Query("SELECT * FROM sync_queue WHERE submission_id = :submissionId")
    fun getQueueItemsBySubmissionId(submissionId: Int): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY created_at ASC")
    fun getQueueItemsByStatus(status: String): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue ORDER BY created_at ASC")
    fun getAllQueueItems(): Flow<List<SyncQueueEntity>>

    // ============================================
    // QUERIES FOR SYNC WORKER
    // ============================================

    // Get items ready to sync (next_retry_time <= now)
    @Query("""
        SELECT * FROM sync_queue 
        WHERE status != 'SYNCED' 
        AND next_retry_time <= :currentTime
        AND retry_count < max_retries
        ORDER BY created_at ASC 
        LIMIT :limit
    """)
    suspend fun getItemsReadyForSync(currentTime: Long = System.currentTimeMillis(), limit: Int = 20): List<SyncQueueEntity>

    // Get all pending items
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingItems(limit: Int = 50): List<SyncQueueEntity>

    // Mark as syncing
    @Query("UPDATE sync_queue SET status = 'SYNCING', last_attempt_at = :timestamp WHERE id = :queueId")
    suspend fun markAsSyncing(queueId: Int, timestamp: Long = System.currentTimeMillis())

    // Mark as synced
    @Query("UPDATE sync_queue SET status = 'SYNCED', last_attempt_at = :timestamp WHERE id = :queueId")
    suspend fun markAsSynced(queueId: Int, timestamp: Long = System.currentTimeMillis())

    // Mark as failed with retry logic
    @Query("""
        UPDATE sync_queue 
        SET status = 'FAILED', 
            error_message = :errorMsg,
            retry_count = retry_count + 1,
            last_attempt_at = :timestamp,
            next_retry_time = :nextRetryTime
        WHERE id = :queueId
    """)
    suspend fun markAsFailed(queueId: Int, errorMsg: String, nextRetryTime: Long, timestamp: Long = System.currentTimeMillis())

    // ============================================
    // COUNT OPERATIONS
    // ============================================

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status != 'SYNCED'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM sync_queue WHERE submission_id = :submissionId AND status != 'SYNCED'")
    suspend fun countPendingForSubmission(submissionId: Int): Int

    // ============================================
    // DELETE OPERATIONS
    // ============================================

    @Query("DELETE FROM sync_queue WHERE id = :queueId")
    suspend fun deleteQueueItemById(queueId: Int)

    @Query("DELETE FROM sync_queue WHERE submission_id = :submissionId")
    suspend fun deleteQueueItemsBySubmissionId(submissionId: Int)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED' AND last_attempt_at < :beforeTime")
    suspend fun deleteSyncedBefore(beforeTime: Long)

    // ============================================
    // BULK OPERATIONS
    // ============================================

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun deleteSyncedItems()

    @Query("UPDATE sync_queue SET retry_count = 0 WHERE status = 'FAILED'")
    suspend fun resetFailedRetries()
}