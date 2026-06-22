package com.jsac.sync.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsac.sync.data.local.db.entity.FormEntity
import com.jsac.sync.data.repository.FormRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: FormRepository
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val forms: List<FormEntity>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        Log.d("DashboardViewModel", "🎬 Initializing DashboardViewModel")
        loadForms()
    }

    /**
     * Load forms from repository
     * Uses offline-first pattern:
     * 1. Check local cache
     * 2. Show cached data
     * 3. Fetch from API
     * 4. Update cache
     */
    fun loadForms() {
        Log.d("DashboardViewModel", "📥 Loading forms...")

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            repository.getFormsList().collect { result ->
                result.onSuccess { forms ->
                    Log.d("DashboardViewModel", "✅ Forms loaded: ${forms.size} forms")
                    _uiState.value = UiState.Success(forms)

                }.onFailure { error ->
                    Log.e("DashboardViewModel", "❌ Error: ${error.message}", error)
                    _uiState.value = UiState.Error(error.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Refresh forms - clears cache and fetches fresh data
     * ✅ Call this when user pulls-to-refresh
     */
    fun refreshForms() {
        Log.d("DashboardViewModel", "🔄 Refreshing forms...")

        viewModelScope.launch {
            try {
                // ✅ IMPORTANT: Clear cache before loading

                Log.d("DashboardViewModel", "🗑️ Cache cleared")

                _uiState.value = UiState.Loading

                // Now load fresh data from API
                repository.getFormsList().collect { result ->
                    result.onSuccess { forms ->
                        Log.d("DashboardViewModel", "✅ Fresh forms loaded: ${forms.size} forms")
                        _uiState.value = UiState.Success(forms)

                    }.onFailure { error ->
                        Log.e("DashboardViewModel", "❌ Refresh error: ${error.message}", error)
                        _uiState.value = UiState.Error(error.message ?: "Refresh failed")
                    }
                }

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "❌ Exception during refresh: ${e.message}", e)
                _uiState.value = UiState.Error("Refresh failed: ${e.message}")
            }
        }
    }

    /**
     * Retry loading forms after error
     */
    fun retryLoadForms() {
        Log.d("DashboardViewModel", "🔄 Retrying forms load...")
        loadForms()
    }
}