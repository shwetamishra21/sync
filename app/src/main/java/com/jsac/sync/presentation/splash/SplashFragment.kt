package com.jsac.sync.presentation.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {

            viewModel.isLoggedIn.collect { isLoggedIn ->

                Handler(Looper.getMainLooper()).postDelayed({

                    if (isLoggedIn) {

                        findNavController().navigate(
                            R.id.action_splash_to_home
                        )

                    } else {

                        findNavController().navigate(
                            R.id.action_splash_to_home
                        )
                    }

                }, 2000)
            }
        }
    }
}