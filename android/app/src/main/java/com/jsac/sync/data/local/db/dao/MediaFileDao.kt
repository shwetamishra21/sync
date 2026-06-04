package com.jsac.sync.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jsac.sync.data.local.db.entity.MediaFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaFileDao {

    // ============================================
    // WRITE OPERATIONS
    // ============================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaFile(mediaFile: MediaFileEntity): Long

    @Update
    suspend fun updateMediaFile(mediaFile: MediaFileEntity)

    @Delete
    suspend fun deleteMediaFile(mediaFile: MediaFileEntity)

    // ============================================
    // READ OPERATIONS
    // ============================================

    @Query("SELECT * FROM media_files WHERE id = :mediaId")
    fun getMediaFileById(mediaId: Int): Flow<MediaFileEntity?>

    @Query("SELECT * FROM media_files WHERE submission_id = :submissionId ORDER BY created_at ASC")
    fun getMediaFilesBySubmissionId(submissionId: Int): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE upload_status = :status ORDER BY created_at ASC")
    fun getMediaFilesByStatus(status: String): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE upload_status = :status LIMIT :limit")
    suspend fun getMediaFilesByStatusOnce(status: String, limit: Int = 50): List<MediaFileEntity>

    @Query("SELECT * FROM media_files ORDER BY created_at DESC")
    fun getAllMediaFiles(): Flow<List<MediaFileEntity>>

    // ============================================
    // QUERIES FOR UPLOAD
    // ============================================

    @Query("SELECT * FROM media_files WHERE upload_status = 'LOCAL' OR upload_status = 'FAILED' ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingUploadFiles(limit: Int = 20): List<MediaFileEntity>

    @Query("UPDATE media_files SET upload_status = 'UPLOADING' WHERE id = :mediaId")
    suspend fun markAsUploading(mediaId: Int)

    @Query("UPDATE media_files SET upload_status = 'UPLOADED', server_url = :serverUrl, uploaded_at = :timestamp WHERE id = :mediaId")
    suspend fun markAsUploaded(mediaId: Int, serverUrl: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE media_files SET upload_status = 'FAILED' WHERE id = :mediaId")
    suspend fun markAsUploadFailed(mediaId: Int)

    // ============================================
    // COUNT OPERATIONS
    // ============================================

    @Query("SELECT COUNT(*) FROM media_files WHERE submission_id = :submissionId")
    fun getMediaCountForSubmission(submissionId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_files WHERE upload_status = :status")
    suspend fun countByStatus(status: String): Int

    // ============================================
    // DELETE OPERATIONS
    // ============================================

    @Query("DELETE FROM media_files WHERE id = :mediaId")
    suspend fun deleteMediaFileById(mediaId: Int)

    @Query("DELETE FROM media_files WHERE submission_id = :submissionId")
    suspend fun deleteMediaFilesBySubmissionId(submissionId: Int)

    @Query("DELETE FROM media_files WHERE upload_status = 'UPLOADED' AND uploaded_at < :beforeTime")
    suspend fun deleteUploadedBefore(beforeTime: Long)
}