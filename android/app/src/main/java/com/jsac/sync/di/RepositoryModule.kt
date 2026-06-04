package com.jsac.sync.di

import com.jsac.sync.data.repository.FormRepository
import com.jsac.sync.data.repository.FormSubmissionRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt Module for Repository bindings
 *
 * Provides singleton instances of all repositories
 * Automatically injects dependencies (DAOs, APIs)
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repositories are provided via @Inject constructors
    // No explicit @Provides needed - Hilt auto-discovers them

    // FormRepository @Inject constructor(...) in data/repository/FormRepository.kt
    // FormSubmissionRepository @Inject constructor(...) in data/repository/FormSubmissionRepository.kt
}