package com.jsac.sync.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jsac.sync.data.local.db.dao.FormDao
import com.jsac.sync.data.local.db.entity.FormEntity
import com.jsac.sync.data.local.db.entity.FormFieldEntity

@Database(
    entities = [
        FormEntity::class,
        FormFieldEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun formDao(): FormDao

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
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}