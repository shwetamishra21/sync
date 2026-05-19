package com.jsac.sync.presentation.home

import androidx.lifecycle.ViewModel
import com.jsac.sync.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HealthRepository
) : ViewModel() {

    suspend fun checkHealth(): Response<com.jsac.sync.data.remote.dto.HealthResponse> {

        return repository.checkHealth()
    }
}