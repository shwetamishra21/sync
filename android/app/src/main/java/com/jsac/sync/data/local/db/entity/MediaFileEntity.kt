package com.jsac.sync.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing media files (photos, documents)
 *
 * WHY: Track uploaded files and their sync status
 *
 * Purpose:
 * - Store local file paths for photos/documents
 * - Track upload status of each file
 * - Link files to specific form submissions
 * - Support multiple files per form field
 */
@Entity(
    tableName = "media_files",
    foreignKeys = [
        ForeignKey(
            entity = FormSubmissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["submission_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("submission_id"),
        Index("upload_status")
    ]
)
data class MediaFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val submission_id: Int,           // Which submission this file belongs to
    val field_id: String,             // Which form field this file is for

    val local_path: String,           // Path to file on device
    val server_url: String? = null,   // URL on server after upload

    val file_name: String,            // Original file name
    val file_size: Long,              // File size in bytes
    val file_type: String,            // MIME type (image/jpeg, etc)

    val upload_status: String,        // LOCAL, UPLOADING, UPLOADED, FAILED
    val created_at: Long = System.currentTimeMillis(),
    val uploaded_at: Long? = null
)

// Upload status constants
object UploadStatus {
    const val LOCAL = "LOCAL"          // File exists on device only
    const val UPLOADING = "UPLOADING"  // Currently uploading
    const val UPLOADED = "UPLOADED"    // Successfully uploaded
    const val FAILED = "FAILED"        // Failed to upload
}