package com.jsac.sync.di

import android.content.Context
import com.jsac.sync.data.local.db.AppDatabase
import com.jsac.sync.data.local.db.dao.FormDao
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
}