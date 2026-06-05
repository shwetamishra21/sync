package com.jsac.sync.di

import com.jsac.sync.data.repository.AuthRepository
import com.jsac.sync.data.repository.ForgotPasswordRepository
import com.jsac.sync.data.repository.FormRepository
import com.jsac.sync.data.repository.FormSubmissionRepository
import com.jsac.sync.data.repository.HealthRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt Module for providing repository instances
 * All repositories are automatically created and injected
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repositories are provided automatically via constructor injection
    // This module is here for organization and future expansion
    //
    // To use in a ViewModel:
    //   @HiltViewModel
    //   class MyViewModel @Inject constructor(
    //       private val formSubmissionRepository: FormSubmissionRepository
    //   ) : ViewModel() { ... }
}