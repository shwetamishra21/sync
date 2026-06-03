package com.jsac.sync.presentation.dashboard

import android.os.Bundle
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
import com.jsac.sync.R
import com.jsac.sync.presentation.dashboard.adapter.FormListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var rvForms: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var containerEmpty: LinearLayout

    private lateinit var formAdapter: FormListAdapter

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        rvForms = view.findViewById(R.id.rvForms)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        containerEmpty = view.findViewById(R.id.containerEmpty)

        setupRecyclerView()
        observeUiState()
    }

    private fun setupRecyclerView() {
        formAdapter = FormListAdapter { formId, formName ->
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
                Toast.makeText(requireContext(), "Navigation not ready yet", Toast.LENGTH_SHORT).show()
            }
        }

        rvForms.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = formAdapter
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is DashboardViewModel.UiState.Loading -> {
                        showLoading()
                    }
                    is DashboardViewModel.UiState.Success -> {
                        hideLoading()
                        if (state.forms.isEmpty()) {
                            showEmpty()
                        } else {
                            showForms(state.forms)
                        }
                    }
                    is DashboardViewModel.UiState.Error -> {
                        hideLoading()
                        showError(state.message)
                    }
                }
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
        rvForms.visibility = View.VISIBLE
        tvError.visibility = View.GONE
        containerEmpty.visibility = View.GONE
        formAdapter.submitList(forms)
    }

    private fun showEmpty() {
        rvForms.visibility = View.GONE
        tvError.visibility = View.GONE
        containerEmpty.visibility = View.VISIBLE
        tvEmpty.text = "No forms available"
    }

    private fun showError(message: String) {
        rvForms.visibility = View.GONE
        containerEmpty.visibility = View.GONE
        tvError.visibility = View.VISIBLE
        tvError.text = "Error: $message"

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}