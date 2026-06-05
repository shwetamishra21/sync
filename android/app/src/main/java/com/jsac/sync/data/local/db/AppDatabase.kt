package com.jsac.sync.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jsac.sync.data.local.db.dao.FormDao
import com.jsac.sync.data.local.db.dao.FormSubmissionDao
import com.jsac.sync.data.local.db.dao.MediaFileDao
import com.jsac.sync.data.local.db.entity.FormEntity
import com.jsac.sync.data.local.db.entity.FormFieldEntity
import com.jsac.sync.data.local.db.entity.FormSubmissionEntity
import com.jsac.sync.data.local.db.entity.MediaFileEntity

/**
 * Room Database for JSAC Sync
 *
 * Tables:
 * - forms: Form metadata (from API)
 * - form_fields: Form field definitions
 * - form_submissions: User-submitted form data
 * - media_files: Photos and documents
 *
 * Version 2: Added form submissions and media files for offline support
 */
@Database(
    entities = [
        FormEntity::class,
        FormFieldEntity::class,
        FormSubmissionEntity::class,
        MediaFileEntity::class
    ],
    version = 2,
    exportSchema = false  // ✅ FIXED: Set to false for development
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun formDao(): FormDao
    abstract fun formSubmissionDao(): FormSubmissionDao
    abstract fun mediaFileDao(): MediaFileDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also {
                    instance = it
                }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "jsac_sync_db"
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * Migration from v1 to v2
         * Adds: form_submissions and media_files tables
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create form_submissions table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `form_submissions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `form_id` TEXT NOT NULL,
                        `form_data` TEXT NOT NULL,
                        `sync_status` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `synced_at` INTEGER,
                        `error_message` TEXT,
                        `retry_count` INTEGER NOT NULL DEFAULT 0,
                        `last_sync_attempt` INTEGER,
                        FOREIGN KEY(`form_id`) REFERENCES `forms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // Create index for form_submissions
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_form_submissions_form_id` ON `form_submissions` (`form_id`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_form_submissions_sync_status` ON `form_submissions` (`sync_status`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_form_submissions_created_at` ON `form_submissions` (`created_at`)"
                )

                // Create media_files table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `media_files` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `submission_id` INTEGER NOT NULL,
                        `field_id` TEXT NOT NULL,
                        `local_path` TEXT NOT NULL,
                        `server_url` TEXT,
                        `file_name` TEXT NOT NULL,
                        `file_size` INTEGER NOT NULL,
                        `file_type` TEXT NOT NULL,
                        `upload_status` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `uploaded_at` INTEGER,
                        FOREIGN KEY(`submission_id`) REFERENCES `form_submissions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // Create index for media_files
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_media_files_submission_id` ON `media_files` (`submission_id`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_media_files_upload_status` ON `media_files` (`upload_status`)"
                )
            }
        }
    }
}