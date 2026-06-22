package com.jsac.sync.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing form submissions locally
 *
 * WHY: Offline-first design - user can fill forms offline and sync when online
 *
 * Purpose:
 * - Store user-submitted form data locally
 * - Track sync status (PENDING, SYNCING, SYNCED, FAILED)
 * - Enable offline form filling
 * - Queue forms for background sync
 */
@Entity(
    tableName = "form_submissions",
    foreignKeys = [
        ForeignKey(
            entity = FormEntity::class,
            parentColumns = ["id"],
            childColumns = ["form_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("form_id"),
        Index("sync_status"),
        Index("created_at")
    ]
)
data class FormSubmissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val form_id: String,              // Which form was submitted
    val form_data: String,            // JSON string of all form values
    val sync_status: String,          // PENDING, SYNCING, SYNCED, FAILED

    // Metadata
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val synced_at: Long? = null,      // When it was successfully synced

    // Error tracking
    val error_message: String? = null, // Error if sync failed
    val retry_count: Int = 0,         // How many times we tried to sync
    val last_sync_attempt: Long? = null
)

// Sync status constants
object SyncStatus {
    const val PENDING = "PENDING"      // Not yet submitted to server
    const val SYNCING = "SYNCING"      // Currently uploading
    const val SYNCED = "SYNCED"        // Successfully uploaded
    const val FAILED = "FAILED"        // Failed to upload (will retry)
}