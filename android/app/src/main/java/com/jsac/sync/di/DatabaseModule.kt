package com.jsac.sync.di

import android.content.Context
import com.jsac.sync.data.local.db.AppDatabase
import com.jsac.sync.data.local.db.dao.FormDao
import com.jsac.sync.data.local.db.dao.FormSubmissionDao
import com.jsac.sync.data.local.db.dao.MediaFileDao
import com.jsac.sync.data.local.db.dao.SyncQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideFormDao(
        database: AppDatabase
    ): FormDao {
        return database.formDao()
    }

    @Provides
    @Singleton
    fun provideFormSubmissionDao(
        database: AppDatabase
    ): FormSubmissionDao {
        return database.formSubmissionDao()
    }

    @Provides
    @Singleton
    fun provideMediaFileDao(
        database: AppDatabase
    ): MediaFileDao {
        return database.mediaFileDao()
    }

    @Provides
    @Singleton
    fun provideSyncQueueDao(
        database: AppDatabase
    ): SyncQueueDao {
        return database.syncQueueDao()
    }
}