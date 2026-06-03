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
import com.jsac.sync.utils.EmailValidator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail =
            view.findViewById<EditText>(R.id.etUsername)  // Changed to Email

        val etPassword =
            view.findViewById<EditText>(R.id.etPassword)

        val btnLogin =
            view.findViewById<Button>(R.id.btnLogin)

        val btnGoToRegister =
            view.findViewById<Button>(R.id.btnGoToRegister)

        val btnForgotPassword =
            view.findViewById<Button>(R.id.btnForgotPassword)

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            // Validate email
            val emailError = EmailValidator.getEmailErrorMessage(email)
            if (emailError != null) {
                Toast.makeText(requireContext(), emailError, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate password
            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ FIXED (FIX #6): Disable button during login to prevent multiple clicks
            btnLogin.isEnabled = false
            btnLogin.text = "Logging in..."

            viewModel.login(
                email,
                password,
                onSuccess = {
                    // ✅ Re-enable button on success
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"

                    Toast.makeText(
                        requireContext(),
                        "Login Successful",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(
                        R.id.action_login_to_home
                    )
                },
                onError = { error ->
                    // ✅ Re-enable button on error
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"

                    Toast.makeText(
                        requireContext(),
                        error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        btnGoToRegister.setOnClickListener {
            findNavController().navigate(
                R.id.action_login_to_register
            )
        }

        // NEW: Navigate to Forgot Password
        btnForgotPassword.setOnClickListener {
            findNavController().navigate(
                R.id.action_login_to_forgot_password
            )
        }
    }
}