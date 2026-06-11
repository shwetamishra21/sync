package com.jsac.sync.presentation.submissions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsac.sync.data.local.db.entity.FormSubmissionEntity
import com.jsac.sync.data.repository.FormSubmissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for submissions list screen
 *
 * Manages:
 * - Loading submissions from Room
 * - Filtering by sync status
 * - Refreshing the list
 * - Deleting submissions
 */
@HiltViewModel
class SubmissionsViewModel @Inject constructor(
    private val repository: FormSubmissionRepository
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val submissions: List<FormSubmissionEntity>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _submissions = MutableStateFlow<UiState>(UiState.Loading)
    val submissions: StateFlow<UiState> = _submissions.asStateFlow()

    // Current filter
    private var currentFilter: String? = null

    init {
        Log.d("SubmissionsViewModel", "🎬 Initializing")
        loadSubmissions(null)
    }

    /**
     * Load submissions from repository
     *
     * @param status Filter by sync status (PENDING, SYNCING, SYNCED, FAILED) or null for all
     */
    fun loadSubmissions(status: String? = null) {
        Log.d("SubmissionsViewModel", "📥 Loading submissions (filter: $status)")

        currentFilter = status

        viewModelScope.launch {
            _submissions.value = UiState.Loading

            try {
                val submissions = if (status == null) {
                    // Get all submissions
                    repository.getAllSubmissions()
                } else {
                    // Get submissions by status
                    repository.getSubmissionsByStatus(status)
                }

                // Collect and emit
                submissions.collect { submissionList ->
                    Log.d("SubmissionsViewModel", "✅ Loaded ${submissionList.size} submissions")
                    _submissions.value = UiState.Success(submissionList)
                }

            } catch (e: Exception) {
                Log.e("SubmissionsViewModel", "❌ Error loading submissions: ${e.message}", e)
                _submissions.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Refresh submissions (clear and reload)
     */
    fun refreshSubmissions(status: String? = null) {
        Log.d("SubmissionsViewModel", "🔄 Refreshing submissions")
        loadSubmissions(status)
    }

    /**
     * Delete a submission
     */
    fun deleteSubmission(submissionId: Int) {
        Log.d("SubmissionsViewModel", "🗑️ Deleting submission: $submissionId")

        viewModelScope.launch {
            try {
                repository.deleteSubmission(submissionId)
                Log.d("SubmissionsViewModel", "✅ Submission deleted")

                // Refresh list
                loadSubmissions(currentFilter)

            } catch (e: Exception) {
                Log.e("SubmissionsViewModel", "❌ Error deleting submission: ${e.message}", e)
                _submissions.value = UiState.Error("Failed to delete: ${e.message}")
            }
        }
    }

    /**
     * Get submission count for a status
     */
    suspend fun getSubmissionCount(status: String): Int {
        return try {
            repository.countByStatus(status)
        } catch (e: Exception) {
            Log.e("SubmissionsViewModel", "Error getting count: ${e.message}")
            0
        }
    }
}