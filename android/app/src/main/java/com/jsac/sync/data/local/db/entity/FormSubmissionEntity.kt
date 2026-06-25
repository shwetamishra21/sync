package com.jsac.sync.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID


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
        Index("created_at"),
        Index("idempotency_key")
    ]
)
data class FormSubmissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val form_id: String,
    val form_data: String,
    val sync_status: String,

    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String = UUID.randomUUID().toString(),

    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val synced_at: Long? = null,

    val error_message: String? = null,
    val retry_count: Int = 0,
    val last_sync_attempt: Long? = null
)

object SyncStatus {
    const val PENDING = "PENDING"
    const val SYNCING = "SYNCING"
    const val SYNCED = "SYNCED"
    const val FAILED = "FAILED"
}