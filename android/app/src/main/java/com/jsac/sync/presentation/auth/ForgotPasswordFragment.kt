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

/**
 * ✅ FIXED: Updated to pass 'username' parameter to ViewModel
 * This aligns with the backend's expectation of 'username' field
 */
@AndroidEntryPoint
class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etForgotEmail)
        val btnSendReset = view.findViewById<Button>(R.id.btnSendReset)
        val btnBackToLogin = view.findViewById<Button>(R.id.btnBackToLogin)

        btnSendReset.setOnClickListener {

            val email = etEmail.text.toString().trim()

            // Validate email
            val emailError = EmailValidator.getEmailErrorMessage(email)
            if (emailError != null) {
                Toast.makeText(requireContext(), emailError, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ FIXED: Pass email as 'username' parameter to match backend
            viewModel.requestPasswordReset(
                email,  // This is the username from the backend perspective
                onSuccess = {
                    Toast.makeText(
                        requireContext(),
                        "Reset link sent to your email",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate to reset password screen with the username
                    val bundle = Bundle().apply {
                        putString("username", email)  // ✅ FIXED: Pass as 'username'
                    }

                    findNavController().navigate(
                        R.id.action_forgot_password_to_reset_password,
                        bundle
                    )
                },
                onError = { error ->
                    Toast.makeText(
                        requireContext(),
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        btnBackToLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}