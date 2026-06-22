package com.jsac.sync.presentation.home

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jsac.sync.R
import com.jsac.sync.presentation.sync.SyncStatusViewModel
import com.jsac.sync.worker.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Updated Home Fragment with automatic sync
 *
 * Features:
 * - Triggers form/media sync when user opens app
 * - Shows pending items count
 * - Shows sync status
 * - Manual sync button
 * - View saved submissions button
 */
@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()
    private val syncStatusViewModel: SyncStatusViewModel by viewModels()

    // UI Components
    private lateinit var tvPendingCount: TextView
    private lateinit var tvSyncStatus: TextView
    private lateinit var progressBarSync: ProgressBar
    private lateinit var btnManualSync: Button
    private lateinit var btnViewForms: Button
    private lateinit var btnViewSubmissions: Button
    private lateinit var btnLogout: Button

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("HomeFragment", "🏠 Home screen loaded")

        // Find views
        tvPendingCount = view.findViewById(R.id.tvPendingCount)
        tvSyncStatus = view.findViewById(R.id.tvSyncStatus)
        progressBarSync = view.findViewById(R.id.progressBarSync)
        btnManualSync = view.findViewById(R.id.btnManualSync)
        btnViewForms = view.findViewById(R.id.btnViewForms)
        btnViewSubmissions = view.findViewById(R.id.btnViewSubmissions)
        btnLogout = view.findViewById(R.id.btnLogout)

        // ============================================
        // AUTOMATIC SYNC ON OPEN
        // ============================================

        // ============================================
// REFRESH SYNC STATUS ON OPEN
// ============================================

        Log.d("HomeFragment", "🔄 Refreshing sync status...")
        syncStatusViewModel.refreshStatus()

        // ============================================
        // OBSERVE SYNC STATUS
        // ============================================

        lifecycleScope.launch {
            syncStatusViewModel.syncStatus.collect { status ->
                updateSyncUI(status)
            }
        }

        // ============================================
        // BUTTON LISTENERS
        // ============================================

        btnViewForms.setOnClickListener {
            Log.d("HomeFragment", "📋 Navigate to dashboard")
            try {
                findNavController().navigate(R.id.action_home_to_dashboard)
            } catch (e: Exception) {
                Log.e("HomeFragment", "❌ Navigation error: ${e.message}", e)
            }
        }

        btnViewSubmissions.setOnClickListener {
            Log.d("HomeFragment", "📂 View Submissions button clicked")
            navigateToSubmissions()
        }

        btnManualSync.setOnClickListener {
            Log.d("HomeFragment", "🚀 Manual sync triggered")
            syncStatusViewModel.triggerSync(requireContext())
        }

        btnLogout.setOnClickListener {
            Log.d("HomeFragment", "🚪 Logout button clicked")
            viewModel.logout()

            try {
                findNavController().navigate(R.id.action_home_to_login)
                Log.d("HomeFragment", "✅ Navigated to Login after logout")
            } catch (e: Exception) {
                Log.e("HomeFragment", "❌ Navigation error: ${e.message}", e)
            }
        }
    }

    /**
     * Navigate to saved submissions screen
     */
    private fun navigateToSubmissions() {
        Log.d("HomeFragment", "→ Navigating to Submissions List")

        try {
            findNavController().navigate(R.id.action_home_to_submissions)
            Log.d("HomeFragment", "✅ Navigation successful")
        } catch (e: IllegalArgumentException) {
            Log.e(
                "HomeFragment",
                "❌ Navigation action not found: ${e.message}"
            )

            Toast.makeText(
                requireContext(),
                "Cannot navigate to submissions. Action not found.",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e(
                "HomeFragment",
                "❌ Navigation error: ${e.message}",
                e
            )

            Toast.makeText(
                requireContext(),
                "Navigation error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Update UI based on sync status
     */
    private fun updateSyncUI(status: SyncStatusViewModel.SyncStatus) {
        Log.d("HomeFragment", "📊 Updating sync UI")

        if (status.pendingFormCount > 0) {
            tvPendingCount.visibility = View.VISIBLE
            tvPendingCount.text = "📋 ${status.pendingFormCount} pending form(s)"
        } else {
            tvPendingCount.visibility = View.GONE
        }

        tvSyncStatus.text = status.message

        if (status.isSyncing) {
            progressBarSync.visibility = View.VISIBLE
            btnManualSync.isEnabled = false
            btnManualSync.alpha = 0.5f
        } else {
            progressBarSync.visibility = View.GONE
            btnManualSync.isEnabled = true
            btnManualSync.alpha = 1.0f
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "👀 onResume - Checking sync status")
        syncStatusViewModel.refreshStatus()
    }
}