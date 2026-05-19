package com.jsac.sync

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.jsac.sync.presentation.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Attach XML layout to screen
        setContentView(R.layout.activity_main)

        val txtStatus = findViewById<TextView>(R.id.txtStatus)

        lifecycleScope.launch {

            try {

                val response = viewModel.checkHealth()

                txtStatus.text =
                    "Backend Connected\n\n${response.body()}"

            } catch (e: Exception) {

                txtStatus.text =
                    "Connection Failed\n\n${e.message}"

                e.printStackTrace()
            }
        }
    }
}