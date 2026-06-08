package com.jsac.sync.presentation.dashboard

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
import com.jsac.sync.R
import com.jsac.sync.presentation.dashboard.adapter.FormListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by viewModels()

    // UI Components
    private lateinit var rvForms: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var containerEmpty: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private lateinit var formAdapter: FormListAdapter

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("DashboardFragment", "🎬 DashboardFragment created")

        // ============================================
        // BIND VIEWS
        // ============================================

        rvForms = view.findViewById(R.id.rvForms)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        containerEmpty = view.findViewById(R.id.containerEmpty)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        // ============================================
        // SETUP RECYCLER VIEW
        // ============================================

        setupRecyclerView()

        // ============================================
        // SETUP PULL-TO-REFRESH
        // ============================================

        swipeRefresh.setOnRefreshListener {
            Log.d("DashboardFragment", "🔄 Pull-to-refresh triggered")
            viewModel.refreshForms()  // ✅ Clears cache and fetches fresh
        }

        // ============================================
        // OBSERVE UI STATE
        // ============================================

        observeUiState()
    }

    private fun setupRecyclerView() {
        Log.d("DashboardFragment", "🔧 Setting up RecyclerView")

        formAdapter = FormListAdapter { formId: String, formName: String ->
            Log.d("DashboardFragment", "📋 Form clicked: $formName ($formId)")

            val bundle = Bundle().apply {
                putString("form_id", formId)
                putString("form_name", formName)
            }
            try {
                findNavController().navigate(
                    R.id.action_dashboard_to_form_detail,
                    bundle
                )
            } catch (e: Exception) {
                Log.e("DashboardFragment", "❌ Navigation error: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Navigation not ready yet",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        rvForms.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = formAdapter
        }

        Log.d("DashboardFragment", "✅ RecyclerView configured")
    }

    private fun observeUiState() {
        Log.d("DashboardFragment", "👁️ Observing UI state")

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                Log.d("DashboardFragment", "📊 State changed: ${state::class.simpleName}")

                when (state) {
                    is DashboardViewModel.UiState.Loading -> {
                        Log.d("DashboardFragment", "📥 Loading forms...")
                        showLoading()
                    }

                    is DashboardViewModel.UiState.Success -> {
                        Log.d("DashboardFragment", "✅ Loaded: ${state.forms.size} forms")
                        hideLoading()
                        if (state.forms.isEmpty()) {
                            showEmpty()
                        } else {
                            showForms(state.forms)
                        }
                    }

                    is DashboardViewModel.UiState.Error -> {
                        Log.e("DashboardFragment", "❌ Error: ${state.message}")
                        hideLoading()
                        showError(state.message)
                    }
                }

                // ✅ Update refresh animation
                swipeRefresh.isRefreshing = (state is DashboardViewModel.UiState.Loading)
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        rvForms.visibility = View.GONE
        tvError.visibility = View.GONE
        containerEmpty.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun showForms(forms: List<com.jsac.sync.data.local.db.entity.FormEntity>) {
        Log.d("DashboardFragment", "📋 Displaying ${forms.size} forms")

        rvForms.visibility = View.VISIBLE
        tvError.visibility = View.GONE
        containerEmpty.visibility = View.GONE
        formAdapter.submitList(forms)
    }

    private fun showEmpty() {
        Log.d("DashboardFragment", "📭 No forms available")

        rvForms.visibility = View.GONE
        tvError.visibility = View.GONE
        containerEmpty.visibility = View.VISIBLE
        tvEmpty.text = "No forms available"
    }

    private fun showError(message: String) {
        Log.e("DashboardFragment", "🚨 Showing error: $message")

        rvForms.visibility = View.GONE
        containerEmpty.visibility = View.GONE
        tvError.visibility = View.VISIBLE
        tvError.text = "Error: $message"

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}