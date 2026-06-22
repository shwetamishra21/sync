package com.jsac.sync.presentation.submissions

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jsac.sync.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubmissionsListFragment : Fragment(R.layout.fragment_submissions_list) {

    private val viewModel: SubmissionsViewModel by viewModels()

    private lateinit var rvSubmissions: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var containerEmpty: LinearLayout
    private lateinit var spinnerFilter: Spinner
    private lateinit var btnSyncAll: Button

    private lateinit var adapter: SubmissionsAdapter
    private var currentFilter: String? = null
    private var isInitializingSpinner = false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("SubmissionsListFragment", "🎬 Fragment created")

        rvSubmissions = view.findViewById(R.id.rvSubmissions)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        containerEmpty = view.findViewById(R.id.containerEmpty)
        spinnerFilter = view.findViewById(R.id.spinnerFilter)
        btnSyncAll = view.findViewById(R.id.btnSyncAll)

        setupRecyclerView()
        setupFilterSpinner()

        swipeRefresh.setOnRefreshListener {
            Log.d("SubmissionsListFragment", "🔄 Pull-to-refresh triggered")
            viewModel.refreshSubmissions(currentFilter)
        }

        btnSyncAll.setOnClickListener {
            Log.d("SubmissionsListFragment", "🚀 Sync all pending clicked")
            syncAllPending()
        }

        observeSubmissions()
    }

    private fun setupRecyclerView() {
        Log.d("SubmissionsListFragment", "🔧 Setting up RecyclerView")

        adapter = SubmissionsAdapter(
            onSubmissionClick = { submissionId ->
                Log.d("SubmissionsListFragment", "📋 Submission clicked: $submissionId")
                val bundle = Bundle().apply {
                    putInt("submissionId", submissionId)
                }
                try {
                    findNavController().navigate(
                        R.id.action_submissions_to_detail,
                        bundle
                    )
                } catch (e: Exception) {
                    Log.e("SubmissionsListFragment", "Navigation error: ${e.message}")
                    Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT).show()
                }
            },
            onSyncClick = { submissionId ->
                Log.d("SubmissionsListFragment", "⚡ Manual sync for: $submissionId")
                syncSubmission(submissionId)
            },
            onDeleteClick = { submissionId ->
                Log.d("SubmissionsListFragment", "🗑️ Delete submission: $submissionId")
                deleteSubmission(submissionId)
            }
        )

        rvSubmissions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SubmissionsListFragment.adapter
        }

        Log.d("SubmissionsListFragment", "✅ RecyclerView configured")
    }

    private fun setupFilterSpinner() {
        Log.d("SubmissionsListFragment", "🔧 Setting up filter spinner")

        val filterOptions = arrayOf(
            "All",
            "Pending",
            "Syncing",
            "Synced",
            "Failed"
        )

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filterOptions
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = spinnerAdapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (isInitializingSpinner) {
                    Log.d("SubmissionsListFragment", "⏭️ Skipping filter change during init")
                    return
                }

                val selected = filterOptions[position]
                Log.d("SubmissionsListFragment", "🔍 Filter changed to: $selected")

                currentFilter = when (selected) {
                    "All" -> null
                    "Pending" -> "PENDING"
                    "Syncing" -> "SYNCING"
                    "Synced" -> "SYNCED"
                    "Failed" -> "FAILED"
                    else -> null
                }

                Log.d("SubmissionsListFragment", "   Loading submissions with filter: $currentFilter")
                viewModel.loadSubmissions(currentFilter)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        isInitializingSpinner = true
        spinnerFilter.setSelection(0)
        isInitializingSpinner = false
        currentFilter = null

        Log.d("SubmissionsListFragment", "✅ Filter spinner set to 'All' by default")
    }

    private fun observeSubmissions() {
        Log.d("SubmissionsListFragment", "👁️ Observing submissions")

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.submissions.collect { state ->
                Log.d("SubmissionsListFragment", "📊 State changed: ${state::class.simpleName}")

                when (state) {
                    is SubmissionsViewModel.UiState.Loading -> {
                        Log.d("SubmissionsListFragment", "📥 Loading...")
                        showLoading()
                    }

                    is SubmissionsViewModel.UiState.Success -> {
                        Log.d("SubmissionsListFragment", "✅ Loaded: ${state.submissions.size} submissions")
                        hideLoading()
                        if (state.submissions.isEmpty()) {
                            showEmpty()
                        } else {
                            showSubmissions(state.submissions)
                        }
                    }

                    is SubmissionsViewModel.UiState.Error -> {
                        Log.e("SubmissionsListFragment", "❌ Error: ${state.message}")
                        hideLoading()
                        showError(state.message)
                    }
                }

                swipeRefresh.isRefreshing = (state is SubmissionsViewModel.UiState.Loading)
            }
        }
    }

    private fun showLoading() {
        Log.d("SubmissionsListFragment", "   Showing loading state")
        progressBar.visibility = View.VISIBLE
        rvSubmissions.visibility = View.GONE
        containerEmpty.visibility = View.GONE
    }

    private fun hideLoading() {
        Log.d("SubmissionsListFragment", "   Hiding loading state")
        progressBar.visibility = View.GONE
    }

    private fun showSubmissions(submissions: List<com.jsac.sync.data.local.db.entity.FormSubmissionEntity>) {
        Log.d("SubmissionsListFragment", "📋 Displaying ${submissions.size} submissions")

        rvSubmissions.visibility = View.VISIBLE
        containerEmpty.visibility = View.GONE

        adapter.submitList(submissions)

        Log.d("SubmissionsListFragment", "   ✅ RecyclerView updated with submissions")
    }

    private fun showEmpty() {
        Log.d("SubmissionsListFragment", "📭 No submissions")

        rvSubmissions.visibility = View.GONE
        containerEmpty.visibility = View.VISIBLE
        tvEmpty.text = "No saved submissions"
    }

    private fun showError(message: String) {
        Log.e("SubmissionsListFragment", "🚨 Error: $message")
        rvSubmissions.visibility = View.GONE
        containerEmpty.visibility = View.VISIBLE
        tvEmpty.text = "Error: $message"

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun syncSubmission(submissionId: Int) {
        Log.d("SubmissionsListFragment", "🚀 Syncing submission #$submissionId")
        viewModel.syncOne(requireContext(), submissionId)
        Toast.makeText(requireContext(), "Sync started for submission #$submissionId", Toast.LENGTH_SHORT).show()
    }

    private fun syncAllPending() {
        Log.d("SubmissionsListFragment", "🚀 Syncing all pending submissions")
        viewModel.syncAllPending(requireContext())
        Toast.makeText(requireContext(), "Sync started for all pending submissions", Toast.LENGTH_SHORT).show()
    }

    private fun deleteSubmission(submissionId: Int) {
        Log.d("SubmissionsListFragment", "🗑️ Deleting submission: $submissionId")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.deleteSubmission(submissionId)
                Toast.makeText(requireContext(), "Submission deleted", Toast.LENGTH_SHORT).show()
                viewModel.refreshSubmissions(currentFilter)

            } catch (e: Exception) {
                Log.e("SubmissionsListFragment", "❌ Delete error: ${e.message}", e)
                Toast.makeText(requireContext(), "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("SubmissionsListFragment", "👀 onResume - Refreshing submissions")
        viewModel.refreshSubmissions(currentFilter)
    }
}