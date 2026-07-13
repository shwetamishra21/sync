package com.jsac.sync.presentation.splash

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jsac.sync.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val viewModel: SplashViewModel by viewModels()
    private var hasNavigated = false  // ← KEY: Track if we've already navigated

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("SplashFragment", "Splash screen displayed")

        lifecycleScope.launch {
            viewModel.isLoggedIn.collect { isLoggedIn ->

                Log.d("SplashFragment", "Session state: $isLoggedIn")

                // ← IMPORTANT: Only proceed if we haven't navigated yet
                if (hasNavigated) {
                    Log.d("SplashFragment", "Navigation already completed")
                    return@collect
                }

                view.postDelayed({

                    // Double-check we haven't navigated
                    if (hasNavigated) {
                        return@postDelayed
                    }

                    try {
                        if (isLoggedIn) {

                            Log.d("SplashFragment", "Navigating to Home")
                            hasNavigated = true  // ← Set flag BEFORE navigating

                            findNavController().navigate(
                                R.id.action_splash_to_home
                            )

                        } else {

                            Log.d("SplashFragment", "Navigating to Login")
                            hasNavigated = true  // ← Set flag BEFORE navigating

                            findNavController().navigate(
                                R.id.action_splash_to_login
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("SplashFragment", "Navigation failed: ${e.message}", e)
                    }

                }, 1500)
            }
        }
    }
}