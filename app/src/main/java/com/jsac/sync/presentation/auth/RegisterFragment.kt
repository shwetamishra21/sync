package com.jsac.sync.presentation.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.jsac.sync.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val etUsername =
            view.findViewById<EditText>(R.id.etUsername)

        val etPassword =
            view.findViewById<EditText>(R.id.etPassword)

        val btnRegister =
            view.findViewById<Button>(R.id.btnRegister)

        val btnGoToLogin =
            view.findViewById<Button>(R.id.btnGoToLogin)

        btnRegister.setOnClickListener {

            val username =
                etUsername.text.toString()

            val password =
                etPassword.text.toString()

            viewModel.register(
                username,
                password
            ) { result ->

                Toast.makeText(
                    requireContext(),
                    result,
                    Toast.LENGTH_SHORT
                ).show()

                if (
                    result.contains(
                        "successful",
                        true
                    )
                ) {

                    findNavController().navigate(
                        R.id.action_register_to_login
                    )
                }
            }
        }

        btnGoToLogin.setOnClickListener {

            findNavController().navigate(
                R.id.action_register_to_login
            )
        }
    }
}