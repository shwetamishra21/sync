package com.jsac.sync.presentation.home

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.jsac.sync.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("HomeFragment", "🏠 Home screen loaded")

        try {
            val btnViewForms = view.findViewById<Button>(R.id.btnViewForms)
            if (btnViewForms != null) {
                btnViewForms.setOnClickListener {
                    Log.d("HomeFragment", "📋 Navigate to dashboard")
                    try {
                        findNavController().navigate(R.id.action_home_to_dashboard)
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "❌ Navigation error: ${e.message}", e)
                    }
                }
            } else {
                Log.w("HomeFragment", "⚠️ btnViewForms not found in layout")
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error finding btnViewForms: ${e.message}", e)
        }

        try {
            val btnLogout = view.findViewById<Button>(R.id.btnLogout)
            if (btnLogout != null) {
                btnLogout.setOnClickListener {
                    Log.d("HomeFragment", "🚪 Logout button clicked")
                    viewModel.logout()

                    try {
                        findNavController().navigate(
                            R.id.action_home_to_login
                        )
                        Log.d("HomeFragment", "✅ Navigated to Login after logout")
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "❌ Navigation error: ${e.message}", e)
                    }
                }
            } else {
                Log.w("HomeFragment", "⚠️ btnLogout not found in layout")
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error finding btnLogout: ${e.message}", e)
        }
    }
}