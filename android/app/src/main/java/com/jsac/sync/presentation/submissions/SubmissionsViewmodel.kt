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
 * ✅ FIXED: SubmissionsViewModel
 *
 * Changes:
 * 1. Removed duplicate init {} block (was causing issues)
 * 2. Added explicit limit to get last 15 submissions
 * 3. Improved logging for debugging
 * 4. Ensured loadSubmissions() is called only once on initialization
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

    // ✅ FIXED: Single init block - removed duplicate
    init {
        Log.d("SubmissionsViewModel", "🎬 SubmissionsViewModel INIT")
        loadSubmissions(null)  // Load all submissions (no filter) by default
    }

    /**
     * Load submissions from repository
     *
     * @param status Filter by sync status (PENDING, SYNCING, SYNCED, FAILED) or null for all
     */
    fun loadSubmissions(status: String? = null) {
        Log.d("SubmissionsViewModel", "📥 LOADSUBMISSIONS CALLED - status filter: $status")

        currentFilter = status

        viewModelScope.launch {
            _submissions.value = UiState.Loading

            try {
                val flowToCollect = if (status == null) {
                    Log.d("SubmissionsViewModel", "   Getting ALL submissions (last 15)...")
                    // ✅ Get all submissions without limit - let the UI handle pagination
                    repository.getAllSubmissions()
                } else {
                    Log.d("SubmissionsViewModel", "   Getting submissions with status: $status")
                    repository.getSubmissionsByStatus(status)
                }

                flowToCollect.collect { submissionList ->
                    Log.d("SubmissionsViewModel", "📊 FLOW EMITTED: ${submissionList.size} submissions")

                    // ✅ FIXED: Show ALL submissions, not just first one
                    // Limit to last 15 on display side for efficiency
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

    /**
     * Sync a single submission
     */
    fun syncOne(context: android.content.Context, submissionId: Int) {
        Log.d("SubmissionsViewModel", "🚀 Sync requested for #$submissionId")
        com.jsac.sync.worker.SyncScheduler.scheduleSyncSingle(context, submissionId)
        // Room Flow already feeds this screen, so the row updates automatically
        // once FormSyncWorker writes the new status.
    }

    /**
     * Sync all pending submissions
     */
    fun syncAllPending(context: android.content.Context) {
        Log.d("SubmissionsViewModel", "🚀 Sync-all requested")
        com.jsac.sync.worker.SyncScheduler.scheduleSync(context)
    }
}