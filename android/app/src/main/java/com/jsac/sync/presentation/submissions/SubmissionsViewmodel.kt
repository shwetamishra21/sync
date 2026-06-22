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

    private var currentFilter: String? = null

    init {
        Log.d("SubmissionsViewModel", "🎬 SubmissionsViewModel INIT")
        loadSubmissions(null)
    }

    fun loadSubmissions(status: String? = null) {
        Log.d("SubmissionsViewModel", "📥 LOADSUBMISSIONS CALLED - status filter: $status")

        currentFilter = status
        viewModelScope.launch {
            _submissions.value = UiState.Loading

            try {
                val flowToCollect = if (status == null) {
                    Log.d("SubmissionsViewModel", "   Getting ALL submissions...")
                    repository.getAllSubmissions()
                } else {
                    Log.d("SubmissionsViewModel", "   Getting submissions with status: $status")
                    repository.getSubmissionsByStatus(status)
                }

                flowToCollect.collect { submissionList ->
                    Log.d("SubmissionsViewModel", "📊 FLOW EMITTED: ${submissionList.size} submissions")

                    val displayList = if (submissionList.size > 15) {
                        Log.d("SubmissionsViewModel", "   Showing last 15 of ${submissionList.size} submissions")
                        submissionList.takeLast(15)
                    } else {
                        Log.d("SubmissionsViewModel", "   Showing all ${submissionList.size} submissions")
                        submissionList
                    }

                    displayList.forEachIndexed { idx, sub ->
                        Log.d("SubmissionsViewModel", "   [$idx] ID=${sub.id}, form=${sub.form_id}, status=${sub.sync_status}")
                    }

                    _submissions.value = UiState.Success(displayList)
                }

            } catch (e: Exception) {
                Log.e("SubmissionsViewModel", "❌ Error loading submissions: ${e.message}", e)
                e.printStackTrace()
                _submissions.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refreshSubmissions(status: String? = null) {
        Log.d("SubmissionsViewModel", "🔄 Refreshing submissions")
        loadSubmissions(status)
    }

    fun deleteSubmission(submissionId: Int) {
        Log.d("SubmissionsViewModel", "🗑️ Deleting submission: $submissionId")

        viewModelScope.launch {
            try {
                repository.deleteSubmission(submissionId)
                Log.d("SubmissionsViewModel", "✅ Submission deleted")
                loadSubmissions(currentFilter)

            } catch (e: Exception) {
                Log.e("SubmissionsViewModel", "❌ Error deleting submission: ${e.message}", e)
                _submissions.value = UiState.Error("Failed to delete: ${e.message}")
            }
        }
    }

    suspend fun getSubmissionCount(status: String): Int {
        return try {
            repository.countByStatus(status)
        } catch (e: Exception) {
            Log.e("SubmissionsViewModel", "Error getting count: ${e.message}")
            0
        }
    }

    fun syncOne(context: android.content.Context, submissionId: Int) {
        Log.d("SubmissionsViewModel", "🚀 Sync requested for #$submissionId")
        com.jsac.sync.worker.SyncScheduler.scheduleSyncSingle(context, submissionId)
    }

    fun syncAllPending(context: android.content.Context) {
        Log.d("SubmissionsViewModel", "🚀 Sync-all requested")
        com.jsac.sync.worker.SyncScheduler.scheduleSync(context)
    }
}