package com.jsac.sync.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for configuring WorkManager
 *
 * WorkManager is Android's background task scheduler
 * - Syncs forms when device goes online
 * - Uploads media files
 * - Respects device battery and network constraints
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
}

/**
 * NOTE: WorkManager also requires configuration in:
 *
 * 1. AndroidManifest.xml - Add this inside <application> tag:
 *
 *    <provider
 *        android:name="androidx.work.impl.WorkManagerInitializer"
 *        android:authorities="${applicationId}.androidx-startup"
 *        android:exported="false"
 *        tools:node="remove" />
 *
 * 2. build.gradle.kts - Already included in dependencies:
 *    implementation("androidx.work:work-runtime-ktx:2.8.1")
 *
 * 3. SyncApplication.kt - Workers are auto-discovered via @HiltWorker
 */