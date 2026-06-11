package com.jsac.sync.presentation.submissions

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.jsac.sync.R
import com.jsac.sync.data.local.db.entity.FormSubmissionEntity
import com.jsac.sync.data.repository.FormSubmissionRepository
import com.jsac.sync.worker.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fragment to display detailed view of a single submission
 *
 * Shows:
 * - Form name and metadata
 * - All form field values
 * - Submission status
 * - Sync history
 * - Error messages (if failed)
 * - Action buttons (sync, delete, back)
 */
@AndroidEntryPoint
class SubmissionDetailFragment : Fragment(R.layout.fragment_submission_detail) {

    @Inject
    lateinit var repository: FormSubmissionRepository

    private var submissionId: Int = 0
    private var submission: FormSubmissionEntity? = null

    // UI Components
    private lateinit var scrollView: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvFormName: TextView
    private lateinit var tvFormId: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var tvSyncStatus: TextView
    private lateinit var tvSyncedAt: TextView
    private lateinit var tvRetryCount: TextView
    private lateinit var tvErrorMessage: TextView
    private lateinit var containerFormData: LinearLayout
    private lateinit var btnSync: Button
    private lateinit var btnDelete: Button
    private lateinit var btnBack: Button

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("SubmissionDetailFragment", "🎬 Fragment created")

        // Get submission ID from arguments
        submissionId = arguments?.getInt("submissionId") ?: 0

        if (submissionId == 0) {
            Log.e("SubmissionDetailFragment", "❌ No submission ID provided")
            Toast.makeText(requireContext(), "Invalid submission ID", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        // ============================================
        // BIND VIEWS
        // ============================================

        scrollView = view.findViewById(R.id.scrollView)
        progressBar = view.findViewById(R.id.progressBar)
        tvFormName = view.findViewById(R.id.tvFormName)
        tvFormId = view.findViewById(R.id.tvFormId)
        tvCreatedAt = view.findViewById(R.id.tvCreatedAt)
        tvSyncStatus = view.findViewById(R.id.tvSyncStatus)
        tvSyncedAt = view.findViewById(R.id.tvSyncedAt)
        tvRetryCount = view.findViewById(R.id.tvRetryCount)
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage)
        containerFormData = view.findViewById(R.id.containerFormData)
        btnSync = view.findViewById(R.id.btnSync)
        btnDelete = view.findViewById(R.id.btnDelete)
        btnBack = view.findViewById(R.id.btnBack)

        // ============================================
        // LOAD SUBMISSION
        // ============================================

        loadSubmission()

        // ============================================
        // SETUP BUTTONS
        // ============================================

        btnSync.setOnClickListener {
            Log.d("SubmissionDetailFragment", "⚡ Sync button clicked")
            syncSubmission()
        }

        btnDelete.setOnClickListener {
            Log.d("SubmissionDetailFragment", "🗑️ Delete button clicked")
            deleteSubmission()
        }

        btnBack.setOnClickListener {
            Log.d("SubmissionDetailFragment", "⬅️ Back button clicked")
            findNavController().popBackStack()
        }
    }

    private fun loadSubmission() {
        Log.d("SubmissionDetailFragment", "📥 Loading submission $submissionId")

        showLoading()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.getSubmissionById(submissionId).collect { submissionEntity ->
                    if (submissionEntity != null) {
                        submission = submissionEntity
                        displaySubmission(submissionEntity)
                        hideLoading()
                    }
                }

            } catch (e: Exception) {
                Log.e("SubmissionDetailFragment", "❌ Error loading: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Error loading submission: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                hideLoading()
            }
        }
    }

    private fun displaySubmission(submission: FormSubmissionEntity) {
        Log.d("SubmissionDetailFragment", "📋 Displaying submission")

        // ============================================
        // FORM METADATA
        // ============================================

        tvFormName.text = "Form ID: ${submission.form_id}"
        tvFormId.text = "Submission ID: #${submission.id}"

        val createdDate = java.text.SimpleDateFormat(
            "MMM dd, yyyy HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date(submission.created_at))
        tvCreatedAt.text = "📅 Created: $createdDate"

        // ============================================
        // SYNC STATUS
        // ============================================

        val statusBadge = when (submission.sync_status) {
            "PENDING" -> "⏳ Pending"
            "SYNCING" -> "🔄 Syncing"
            "SYNCED" -> "✅ Synced"
            "FAILED" -> "❌ Failed"
            else -> submission.sync_status
        }
        tvSyncStatus.text = "Status: $statusBadge"

        // Synced date
        if (submission.synced_at != null) {
            val syncedDate = java.text.SimpleDateFormat(
                "MMM dd, yyyy HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(java.util.Date(submission.synced_at!!))
            tvSyncedAt.text = "✅ Synced: $syncedDate"
            tvSyncedAt.visibility = View.VISIBLE
        } else {
            tvSyncedAt.visibility = View.GONE
        }

        // Retry count
        if (submission.retry_count > 0) {
            tvRetryCount.text = "🔁 Retry count: ${submission.retry_count}"
            tvRetryCount.visibility = View.VISIBLE
        } else {
            tvRetryCount.visibility = View.GONE
        }

        // Error message
        if (submission.error_message != null && submission.error_message!!.isNotEmpty()) {
            tvErrorMessage.text = "⚠️ Error: ${submission.error_message}"
            tvErrorMessage.visibility = View.VISIBLE
        } else {
            tvErrorMessage.visibility = View.GONE
        }

        // ============================================
        // FORM DATA
        // ============================================

        displayFormData(submission.form_data)

        // ============================================
        // UPDATE BUTTON STATES
        // ============================================

        btnSync.isEnabled = submission.sync_status != "SYNCED"
        btnSync.text = if (submission.sync_status == "SYNCED") "✅ Already Synced" else "🔄 Sync Now"
    }

    private fun displayFormData(formDataJson: String) {
        Log.d("SubmissionDetailFragment", "📊 Displaying form data")

        containerFormData.removeAllViews()

        try {
            val gson = Gson()
            val formData = gson.fromJson(formDataJson, Map::class.java)

            if (formData.isEmpty()) {
                val tvEmpty = TextView(requireContext()).apply {
                    text = "No form data"
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 16, 0, 0)
                    }
                }
                containerFormData.addView(tvEmpty)
                return
            }

            for ((key, value) in formData) {
                // Field container
                val fieldContainer = LinearLayout(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 12, 0, 0)
                    }
                    orientation = LinearLayout.VERTICAL
                    background = android.graphics.drawable.ColorDrawable(
                        android.graphics.Color.parseColor("#F5F5F5")
                    )
                    setPadding(12, 12, 12, 12)
                }

                // Field label
                val tvLabel = TextView(requireContext()).apply {
                    text = key.toString()
                    textSize = 12f
                    setTextColor(android.graphics.Color.parseColor("#666666"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 4)
                    }
                    typeface = android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.BOLD)
                }
                fieldContainer.addView(tvLabel)

                // Field value
                val tvValue = TextView(requireContext()).apply {
                    text = value.toString()
                    textSize = 14f
                    setTextColor(android.graphics.Color.parseColor("#000000"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                fieldContainer.addView(tvValue)

                containerFormData.addView(fieldContainer)
            }

        } catch (e: Exception) {
            Log.e("SubmissionDetailFragment", "Error parsing form data: ${e.message}")
            val tvError = TextView(requireContext()).apply {
                text = "Error parsing form data: ${e.message}"
                textSize = 14f
                setTextColor(android.graphics.Color.RED)
            }
            containerFormData.addView(tvError)
        }
    }

    private fun syncSubmission() {
        Log.d("SubmissionDetailFragment", "⚡ Syncing submission $submissionId")

        if (submission == null) {
            Toast.makeText(requireContext(), "Submission not loaded", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            btnSync.isEnabled = false
            btnSync.text = "🔄 Syncing..."

            SyncScheduler.syncSubmission(requireContext(), submissionId)

            Toast.makeText(
                requireContext(),
                "Sync started for submission #$submissionId",
                Toast.LENGTH_SHORT
            ).show()

            // Reload after a delay
            view?.postDelayed({
                loadSubmission()
            }, 1000)

        } catch (e: Exception) {
            Log.e("SubmissionDetailFragment", "❌ Sync error: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "Sync failed: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            btnSync.isEnabled = true
            btnSync.text = "🔄 Sync Now"
        }
    }

    private fun deleteSubmission() {
        Log.d("SubmissionDetailFragment", "🗑️ Deleting submission $submissionId")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.deleteSubmission(submissionId)
                Toast.makeText(
                    requireContext(),
                    "Submission deleted",
                    Toast.LENGTH_SHORT
                ).show()

                findNavController().popBackStack()

            } catch (e: Exception) {
                Log.e("SubmissionDetailFragment", "❌ Delete error: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Delete failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        scrollView.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        scrollView.visibility = View.VISIBLE
    }
}