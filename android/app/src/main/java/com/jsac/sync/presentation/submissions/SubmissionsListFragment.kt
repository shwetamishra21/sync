package com.jsac.sync.presentation.submissions

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.jsac.sync.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubmissionsListFragment : Fragment(R.layout.fragment_submissions_list) {

    private val viewModel: SubmissionsViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvSubmissions: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var loadingContainer: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var containerEmpty: LinearLayout
    private lateinit var chipGroupFilter: ChipGroup
    private lateinit var btnSyncAll: MaterialButton
    private lateinit var btnViewForms: MaterialButton

    private lateinit var adapter: SubmissionsAdapter
    private var currentFilter: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        rvSubmissions = view.findViewById(R.id.rvSubmissions)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        loadingContainer = view.findViewById(R.id.loadingContainer)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        containerEmpty = view.findViewById(R.id.containerEmpty)
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter)
        btnSyncAll = view.findViewById(R.id.btnSyncAll)
        btnViewForms = view.findViewById(R.id.btnViewForms)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        setupRecyclerView()
        setupSwipeRefresh()
        setupFilterChips()

        btnSyncAll.setOnClickListener { syncAllPending() }

        btnViewForms.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_submissions_to_home)
            } catch (e: Exception) {
                Log.e("SubmissionsListFragment", "Navigation error: ${e.message}", e)
            }
        }

        observeSubmissions()
    }

    private fun setupRecyclerView() {
        adapter = SubmissionsAdapter(
            onSubmissionClick = { submissionId ->
                val bundle = Bundle().apply { putInt("submissionId", submissionId) }
                try {
                    findNavController().navigate(R.id.action_submissions_to_detail, bundle)
                } catch (e: Exception) {
                    Log.e("SubmissionsListFragment", "Navigation error: ${e.message}")
                    Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT).show()
                }
            },
            onSyncClick = { submissionId -> syncSubmission(submissionId) },
            onDeleteClick = { submissionId -> deleteSubmission(submissionId) }
        )

        rvSubmissions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SubmissionsListFragment.adapter
            setHasFixedSize(true)
            itemAnimator = null
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.md_primary)
        swipeRefresh.setOnRefreshListener {
            viewModel.refreshSubmissions(currentFilter)
        }
    }

    /**
     * Filter chips replace the previous Spinner. Each chip id maps directly
     * to a sync_status value (or null for "All") — no positional index
     * lookup, so reordering chips in XML can't silently break filtering.
     */
    private fun setupFilterChips() {
        chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener

            currentFilter = when (checkedId) {
                R.id.chipPending -> "PENDING"
                R.id.chipSyncing -> "SYNCING"
                R.id.chipSynced -> "SYNCED"
                R.id.chipFailed -> "FAILED"
                else -> null // chipAll
            }

            Log.d("SubmissionsListFragment", "Filter changed to: $currentFilter")
            viewModel.loadSubmissions(currentFilter)
        }
    }

    private fun observeSubmissions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.submissions.collect { state ->
                when (state) {
                    is SubmissionsViewModel.UiState.Loading -> showLoading()

                    is SubmissionsViewModel.UiState.Success -> {
                        hideLoading()
                        if (state.submissions.isEmpty()) showEmpty() else showSubmissions(state.submissions)
                    }

                    is SubmissionsViewModel.UiState.Error -> {
                        hideLoading()
                        showError(state.message)
                    }
                }

                swipeRefresh.isRefreshing = (state is SubmissionsViewModel.UiState.Loading)
            }
        }
    }

    private fun showLoading() {
        loadingContainer.visibility = View.VISIBLE
        rvSubmissions.visibility = View.GONE
        containerEmpty.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingContainer.visibility = View.GONE
    }

    private fun showSubmissions(submissions: List<com.jsac.sync.data.local.db.entity.FormSubmissionEntity>) {
        rvSubmissions.visibility = View.VISIBLE
        containerEmpty.visibility = View.GONE
        adapter.submitList(submissions)
    }

    private fun showEmpty() {
        rvSubmissions.visibility = View.GONE
        containerEmpty.visibility = View.VISIBLE
        tvEmpty.text = getString(R.string.msg_no_submissions)
    }

    private fun showError(message: String) {
        rvSubmissions.visibility = View.GONE
        containerEmpty.visibility = View.VISIBLE
        tvEmpty.text = getString(R.string.msg_error_loading_failed, message)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun syncSubmission(submissionId: Int) {
        viewModel.syncOne(requireContext(), submissionId)
        Toast.makeText(
            requireContext(),
            getString(R.string.msg_sync_started, submissionId),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun syncAllPending() {
        viewModel.syncAllPending(requireContext())
        Toast.makeText(requireContext(), getString(R.string.msg_sync_started_all), Toast.LENGTH_SHORT).show()
    }

    private fun deleteSubmission(submissionId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.deleteSubmission(submissionId)
                Toast.makeText(requireContext(), getString(R.string.msg_submission_deleted), Toast.LENGTH_SHORT).show()
                viewModel.refreshSubmissions(currentFilter)
            } catch (e: Exception) {
                Log.e("SubmissionsListFragment", "Delete error: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.msg_error_delete_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSubmissions(currentFilter)
    }
}