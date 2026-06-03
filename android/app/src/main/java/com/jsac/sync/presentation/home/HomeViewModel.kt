package com.jsac.sync.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsac.sync.data.local.datastore.SessionManager
import com.jsac.sync.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    suspend fun checkHealth(): Response<com.jsac.sync.data.remote.dto.HealthResponse> {

        return repository.checkHealth()
    }

    fun logout() {

        Log.d("HomeViewModel", "🚪 Logout initiated from HomeViewModel")

        viewModelScope.launch {

            try {
                sessionManager.clearSession()

                Log.d("HomeViewModel", "✅ Session cleared successfully")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Logout error: ${e.message}", e)
            }
        }
    }
}