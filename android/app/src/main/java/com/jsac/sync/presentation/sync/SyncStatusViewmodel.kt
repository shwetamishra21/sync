package com.jsac.sync.presentation.sync

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsac.sync.data.repository.FormSubmissionRepository
import com.jsac.sync.worker.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    private val submissionRepository: FormSubmissionRepository
) : ViewModel() {

    data class SyncStatus(
        val pendingFormCount: Int = 0,
        val isSyncing: Boolean = false,
        val lastSyncTime: Long = 0,
        val message: String = ""
    )

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        loadSyncStatus()
    }

    fun loadSyncStatus() {
        viewModelScope.launch {
            try {
                val pendingForms = submissionRepository.countByStatus("PENDING") +
                        submissionRepository.countByStatus("FAILED")

                val message = if (pendingForms == 0) {
                    "✅ Everything synced!"
                } else {
                    "⏳ $pendingForms form(s) pending"
                }

                _syncStatus.value = _syncStatus.value.copy(
                    pendingFormCount = pendingForms,
                    message = message
                )
            } catch (e: Exception) {
                Log.e("SyncStatusViewModel", "❌ Error: ${e.message}", e)
            }
        }
    }

    fun refreshStatus() = loadSyncStatus()

    /**
     * Real manual sync: schedules FormSyncWorker, then polls until it
     * finishes (or times out) and refreshes the counts.
     */
    fun triggerSync(context: Context) {
        viewModelScope.launch {
            _syncStatus.value = _syncStatus.value.copy(isSyncing = true, message = "🔄 Syncing...")

            SyncScheduler.scheduleSync(context)

            // Poll for completion (max ~30s) instead of faking a delay
            var waited = 0
            while (SyncScheduler.isSyncing(context) && waited < 30_000) {
                delay(1000)
                waited += 1000
            }

            loadSyncStatus()

            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis()
            )
        }
    }
}