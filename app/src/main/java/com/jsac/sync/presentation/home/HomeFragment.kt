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

        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        btnLogout.setOnClickListener {
            Log.d("HomeFragment", "🚪 Logout button clicked")

            viewModel.logout()

            // Navigate back to Login after logout
            try {
                findNavController().navigate(
                    R.id.action_home_to_login
                )
                Log.d("HomeFragment", "✅ Navigated to Login after logout")
            } catch (e: Exception) {
                Log.e("HomeFragment", "❌ Navigation error: ${e.message}", e)
            }
        }
    }
}