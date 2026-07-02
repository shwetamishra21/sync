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
 * ✅ FIXED: Updated to navigate to OTP Verification instead of Reset Password
 * New flow:
 * 1. User enters email
 * 2. Backend sends OTP to email
 * 3. Navigate to OTP Verification
 * 4. User verifies OTP
 * 5. Navigate to Reset Password
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

            // Send password reset request
            viewModel.requestPasswordReset(
                email,
                onSuccess = {
                    Toast.makeText(
                        requireContext(),
                        "OTP sent to your email. Check your inbox.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // ✅ FIXED: Navigate to OTP Verification (not Reset Password)
                    val bundle = Bundle().apply {
                        putString("username", email)
                    }

                    findNavController().navigate(
                        R.id.action_forgot_password_to_otpVerificationFragment,
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