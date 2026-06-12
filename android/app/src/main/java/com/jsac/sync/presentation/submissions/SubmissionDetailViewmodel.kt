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
 * ViewModel for submission detail screen
 *
 * Manages:
 * - Loading a single submission by ID
 * - Displaying all submission metadata
 * - Syncing the submission
 * - Deleting the submission
 */
@HiltViewModel
class SubmissionDetailViewModel @Inject constructor(
    private val repository: FormSubmissionRepository
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val submission: FormSubmissionEntity) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _submissionState = MutableStateFlow<UiState>(UiState.Loading)
    val submissionState: StateFlow<UiState> = _submissionState.asStateFlow()

    /**
     * Load submission by ID
     *
     * @param submissionId The submission ID to load
     */
    fun loadSubmission(submissionId: Int) {
        Log.d("SubmissionDetailViewModel", "📥 Loading submission: $submissionId")

        viewModelScope.launch {
            _submissionState.value = UiState.Loading

            try {
                repository.getSubmissionById(submissionId).collect { submission ->
                    if (submission != null) {
                        Log.d("SubmissionDetailViewModel", "✅ Loaded submission #$submissionId")
                        _submissionState.value = UiState.Success(submission)
                    } else {
                        Log.e("SubmissionDetailViewModel", "❌ Submission not found: $submissionId")
                        _submissionState.value = UiState.Error("Submission not found")
                    }
                }

            } catch (e: Exception) {
                Log.e("SubmissionDetailViewModel", "❌ Error loading submission: ${e.message}", e)
                _submissionState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Refresh the submission (reload from database)
     *
     * @param submissionId The submission ID to refresh
     */
    fun refreshSubmission(submissionId: Int) {
        Log.d("SubmissionDetailViewModel", "🔄 Refreshing submission: $submissionId")
        loadSubmission(submissionId)
    }

    /**
     * Delete the submission
     *
     * @param submissionId The submission ID to delete
     */
    fun deleteSubmission(submissionId: Int) {
        Log.d("SubmissionDetailViewModel", "🗑️ Deleting submission: $submissionId")

        viewModelScope.launch {
            try {
                repository.deleteSubmission(submissionId)
                Log.d("SubmissionDetailViewModel", "✅ Submission deleted")

            } catch (e: Exception) {
                Log.e("SubmissionDetailViewModel", "❌ Error deleting submission: ${e.message}", e)
                _submissionState.value = UiState.Error("Failed to delete: ${e.message}")
            }
        }
    }

    /**
     * Get form name for the submission
     * (can be used to update UI with form details)
     */

}