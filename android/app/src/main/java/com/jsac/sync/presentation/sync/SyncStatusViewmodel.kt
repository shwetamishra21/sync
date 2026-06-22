package com.jsac.sync.presentation.sync

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

/**
 * ViewModel for displaying sync status
 *
 * Shows user:
 * - Number of pending forms
 * - Number of pending media files
 * - Is sync currently running
 * - Last sync time
 */
@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    private val submissionRepository: FormSubmissionRepository
) : ViewModel() {

    data class SyncStatus(
        val pendingFormCount: Int = 0,
        val pendingMediaCount: Int = 0,
        val isSyncing: Boolean = false,
        val lastSyncTime: Long = 0,
        val message: String = ""
    )

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        Log.d("SyncStatusViewModel", "🎬 Initializing")
        loadSyncStatus()
    }

    // ============================================
    // LOAD STATUS
    // ============================================

    fun loadSyncStatus() {
        Log.d("SyncStatusViewModel", "📊 Loading sync status...")

        viewModelScope.launch {
            try {
                // Get counts from repository
                val pendingForms = submissionRepository.countByStatus("PENDING")
                val pendingMedia = submissionRepository.countByStatus("LOCAL")

                Log.d("SyncStatusViewModel", "📈 Pending: $pendingForms forms, $pendingMedia media")

                val message = when {
                    pendingForms == 0 && pendingMedia == 0 -> "✅ Everything synced!"
                    pendingForms > 0 && pendingMedia == 0 -> "⏳ $pendingForms form(s) pending"
                    pendingForms == 0 && pendingMedia > 0 -> "📸 $pendingMedia media file(s) pending"
                    else -> "⏳ $pendingForms form(s) & $pendingMedia media pending"
                }

                _syncStatus.value = _syncStatus.value.copy(
                    pendingFormCount = pendingForms,
                    pendingMediaCount = pendingMedia,
                    message = message
                )

            } catch (e: Exception) {
                Log.e("SyncStatusViewModel", "❌ Error: ${e.message}", e)
            }
        }
    }

    // ============================================
    // REFRESH
    // ============================================

    fun refreshStatus() {
        Log.d("SyncStatusViewModel", "🔄 Refreshing status...")
        loadSyncStatus()
    }

    // ============================================
    // MANUAL SYNC
    // ============================================

    fun triggerSync(context: android.content.Context) {
        Log.d("SyncStatusViewModel", "🚀 Triggering manual sync...")

        viewModelScope.launch {
            try {
                // Update UI to show syncing
                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = true,
                    message = "🔄 Syncing..."
                )

                // Simulate sync delay for demo
                delay(2000)

                // Reload status
                loadSyncStatus()

                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis()
                )

                Log.d("SyncStatusViewModel", "✅ Sync triggered")

            } catch (e: Exception) {
                Log.e("SyncStatusViewModel", "❌ Error: ${e.message}", e)
                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = false,
                    message = "❌ Sync failed: ${e.message}"
                )
            }
        }
    }

    // ============================================
    // SYNC STATS
    // ============================================


    /**
     * Get last sync time formatted
     */

}