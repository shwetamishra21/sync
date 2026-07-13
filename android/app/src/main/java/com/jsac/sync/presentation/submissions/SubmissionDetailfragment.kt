package com.jsac.sync.presentation.submissions

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import com.jsac.sync.R
import com.jsac.sync.data.local.db.entity.FormSubmissionEntity
import com.jsac.sync.data.repository.FormSubmissionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ✅ ENHANCED Fragment to display detailed view of a single submission
 *
 * Features:
 * - Consistent status display using SubmissionStatusUi (shared with list screen)
 * - Clean metadata presentation without emoji prefixes
 * - Design-system-compliant form data rendering
 * - Better loading and error states
 * - Improved sync button feedback
 */
@AndroidEntryPoint
class SubmissionDetailFragment : Fragment(R.layout.fragment_submission_detail) {

    @Inject
    lateinit var repository: FormSubmissionRepository

    private var submissionId: Int = 0
    private var submission: FormSubmissionEntity? = null

    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var loadingContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var tvFormName: TextView
    private lateinit var tvFormId: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var chipStatus: Chip
    private lateinit var tvSyncedAt: TextView
    private lateinit var tvRetryCount: TextView
    private lateinit var tvErrorMessage: TextView
    private lateinit var containerFormData: LinearLayout
    private lateinit var btnSync: MaterialButton
    private lateinit var btnDelete: MaterialButton

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("SubmissionDetailFragment", "Fragment created")

        // Get submission ID from arguments
        submissionId = arguments?.getInt("submissionId") ?: 0

        if (submissionId == 0) {
            Log.e("SubmissionDetailFragment", "No submission ID provided")
            showError("Invalid submission ID")
            return
        }

        // ============================================
        // BIND VIEWS
        // ============================================

        toolbar = view.findViewById(R.id.toolbar)
        loadingContainer = view.findViewById(R.id.loadingContainer)
        scrollView = view.findViewById(R.id.scrollView)
        tvFormName = view.findViewById(R.id.tvFormName)
        tvFormId = view.findViewById(R.id.tvFormId)
        tvCreatedAt = view.findViewById(R.id.tvCreatedAt)
        chipStatus = view.findViewById(R.id.chipStatus)
        tvSyncedAt = view.findViewById(R.id.tvSyncedAt)
        tvRetryCount = view.findViewById(R.id.tvRetryCount)
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage)
        containerFormData = view.findViewById(R.id.containerFormData)
        btnSync = view.findViewById(R.id.btnSync)
        btnDelete = view.findViewById(R.id.btnDelete)

        // ============================================
        // SETUP TOOLBAR
        // ============================================

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // ============================================
        // SETUP BUTTONS
        // ============================================

        btnSync.setOnClickListener { syncSubmission() }
        btnDelete.setOnClickListener { deleteSubmission() }

        // ============================================
        // LOAD SUBMISSION
        // ============================================

        loadSubmission()
    }

    // ============================================
    // LOAD SUBMISSION
    // ============================================

    private fun loadSubmission() {
        Log.d("SubmissionDetailFragment", "Loading submission $submissionId")

        showLoading()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.getSubmissionById(submissionId).collect { submissionEntity ->
                    if (submissionEntity != null) {
                        submission = submissionEntity
                        displaySubmission(submissionEntity)
                        hideLoading()
                    } else {
                        Log.e("SubmissionDetailFragment", "Submission not found: $submissionId")
                        showError("Submission not found")
                        hideLoading()
                    }
                }

            } catch (e: Exception) {
                Log.e("SubmissionDetailFragment", "Error loading submission: ${e.message}", e)
                showError("Failed to load submission")
                hideLoading()
            }
        }
    }

    // ============================================
    // DISPLAY SUBMISSION
    // ============================================

    private fun displaySubmission(submission: FormSubmissionEntity) {
        Log.d("SubmissionDetailFragment", "Displaying submission")

        // ============================================
        // FORM METADATA
        // ============================================

        tvFormName.text = submission.form_id
        tvFormId.text = getString(R.string.label_submission_number, submission.id)
        tvCreatedAt.text = dateFormat.format(Date(submission.created_at))

        // ============================================
        // SYNC STATUS
        // ============================================

        bindStatus(submission)
        updateSyncButton(submission)

        // ============================================
        // SYNCED DATE
        // ============================================

        if (submission.synced_at != null) {
            tvSyncedAt.text = getString(
                R.string.label_synced_date,
                dateFormat.format(Date(submission.synced_at!!))
            )
            tvSyncedAt.visibility = View.VISIBLE
        } else {
            tvSyncedAt.visibility = View.GONE
        }

        // ============================================
        // RETRY COUNT
        // ============================================

        if (submission.retry_count > 0) {
            tvRetryCount.text = getString(
                R.string.label_retries,
                submission.retry_count
            )
            tvRetryCount.visibility = View.VISIBLE
        } else {
            tvRetryCount.visibility = View.GONE
        }

        // ============================================
        // ERROR MESSAGE
        // ============================================

        if (!submission.error_message.isNullOrEmpty()) {
            tvErrorMessage.text = submission.error_message
            tvErrorMessage.visibility = View.VISIBLE
        } else {
            tvErrorMessage.visibility = View.GONE
        }

        // ============================================
        // FORM DATA
        // ============================================

        displayFormData(submission.form_data)
    }

    // ============================================
    // BIND STATUS CHIP
    // ============================================

    private fun bindStatus(submission: FormSubmissionEntity) {
        val statusInfo = SubmissionStatusUi.of(submission.sync_status)

        chipStatus.text = statusInfo.label
        chipStatus.chipBackgroundColor = ContextCompat.getColorStateList(
            requireContext(),
            statusInfo.backgroundColorRes
        )
        chipStatus.setTextColor(
            ContextCompat.getColor(requireContext(), statusInfo.foregroundColorRes)
        )
    }

    // ============================================
    // UPDATE SYNC BUTTON STATE
    // ============================================

    private fun updateSyncButton(submission: FormSubmissionEntity) {
        val synced = SubmissionStatusUi.isSynced(submission.sync_status)

        btnSync.isEnabled = !synced
        btnSync.alpha = if (synced) 0.6f else 1f
        btnSync.text = if (synced) {
            getString(R.string.status_synced)
        } else {
            getString(R.string.btn_sync_now)
        }

        btnSync.icon = if (synced) {
            null
        } else {
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_sync)
        }
    }

    // ============================================
    // DISPLAY FORM DATA (FIELDS)
    // ============================================

    private fun displayFormData(formDataJson: String) {
        Log.d("SubmissionDetailFragment", "Displaying form data")

        containerFormData.removeAllViews()

        try {
            val gson = Gson()
            val formData = gson.fromJson(formDataJson, Map::class.java)

            if (formData.isEmpty()) {
                val tvEmpty = TextView(requireContext()).apply {
                    text = getString(R.string.msg_no_form_data)
                    setTextAppearance(R.style.TextAppearance_Sync_BodySmall)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = requireContext().resources.getDimensionPixelSize(R.dimen.space_md)
                    }
                }
                containerFormData.addView(tvEmpty)
                return
            }

            for ((key, value) in formData) {
                containerFormData.addView(
                    createFormFieldView(key.toString(), value.toString())
                )
            }

        } catch (e: Exception) {
            Log.e("SubmissionDetailFragment", "Error parsing form data: ${e.message}")
            showError("Error parsing form data")
        }
    }

    // ============================================
    // CREATE FORM FIELD VIEW
    // ============================================

    private fun createFormFieldView(fieldName: String, fieldValue: String): View {
        val fieldContainer = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = requireContext().resources.getDimensionPixelSize(R.dimen.space_sm)
            }
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bg_form_field_container
            ) ?: android.graphics.drawable.ColorDrawable(
                ContextCompat.getColor(requireContext(), R.color.md_surface_variant)
            )
            setPadding(
                requireContext().resources.getDimensionPixelSize(R.dimen.space_md),
                requireContext().resources.getDimensionPixelSize(R.dimen.space_md),
                requireContext().resources.getDimensionPixelSize(R.dimen.space_md),
                requireContext().resources.getDimensionPixelSize(R.dimen.space_md)
            )
        }

        // Field label
        val tvLabel = TextView(requireContext()).apply {
            text = fieldName
            setTextAppearance(R.style.TextAppearance_Sync_BodySmall)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = requireContext().resources.getDimensionPixelSize(R.dimen.space_xs)
            }
        }
        fieldContainer.addView(tvLabel)

        // Field value
        val tvValue = TextView(requireContext()).apply {
            text = fieldValue
            setTextAppearance(R.style.TextAppearance_Sync_Body)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        fieldContainer.addView(tvValue)

        return fieldContainer
    }

    // ============================================
    // SYNC SUBMISSION
    // ============================================

    private fun syncSubmission() {
        Log.d("SubmissionDetailFragment", "Sync requested for #$submissionId")

        com.jsac.sync.worker.SyncScheduler.scheduleSyncSingle(requireContext(), submissionId)
        showToast("Sync started")
    }

    // ============================================
    // DELETE SUBMISSION
    // ============================================

    private fun deleteSubmission() {
        Log.d("SubmissionDetailFragment", "Delete requested for #$submissionId")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.deleteSubmission(submissionId)
                Log.d("SubmissionDetailFragment", "Submission deleted successfully")
                showToast("Submission deleted")
                findNavController().popBackStack()

            } catch (e: Exception) {
                Log.e("SubmissionDetailFragment", "Delete error: ${e.message}", e)
                showError("Delete failed: ${e.message}")
            }
        }
    }

    // ============================================
    // UI STATE MANAGEMENT
    // ============================================

    private fun showLoading() {
        loadingContainer.visibility = View.VISIBLE
        scrollView.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingContainer.visibility = View.GONE
        scrollView.visibility = View.VISIBLE
    }

    // ============================================
    // ERROR & TOAST HELPERS
    // ============================================

    private fun showError(message: String) {
        Log.e("SubmissionDetailFragment", "Error: $message")

        tvErrorMessage.text = message
        tvErrorMessage.visibility = View.VISIBLE

        showToast(message)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}