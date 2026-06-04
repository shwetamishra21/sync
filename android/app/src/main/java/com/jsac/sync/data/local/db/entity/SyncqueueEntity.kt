package com.jsac.sync.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for managing sync queue
 *
 * WHY: Track what needs to be synced and retry failed operations
 *
 * Purpose:
 * - Queue operations that failed and need retry
 * - Track retry count and next retry time (exponential backoff)
 * - Enable background sync worker to find work
 * - Support different operation types (CREATE, UPDATE, DELETE)
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index("status"),
        Index("operation_type"),
        Index("next_retry_time")
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val submission_id: Int,           // Which submission needs syncing
    val operation_type: String,       // CREATE, UPDATE, DELETE
    val status: String,               // PENDING, SYNCING, SYNCED, FAILED

    val retry_count: Int = 0,         // How many times we've tried
    val max_retries: Int = 3,         // Give up after N retries

    val created_at: Long = System.currentTimeMillis(),
    val last_attempt_at: Long? = null,
    val next_retry_time: Long = System.currentTimeMillis(),  // Exponential backoff

    val error_message: String? = null
)

// Operation type constants
object OperationType {
    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"
}

// Queue status constants
object QueueStatus {
    const val PENDING = "PENDING"
    const val SYNCING = "SYNCING"
    const val SYNCED = "SYNCED"
    const val FAILED = "FAILED"
}