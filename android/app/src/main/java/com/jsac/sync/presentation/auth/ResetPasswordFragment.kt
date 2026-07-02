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

/**
 * ✅ FIXED: Updated for OTP-based password reset
 * Changes:
 * 1. Removed Reset Token field (OTP verification already done)
 * 2. Only asks for New Password and Confirm Password
 * 3. Simplified validation
 */
@AndroidEntryPoint
class ResetPasswordFragment : Fragment(R.layout.fragment_reset_password) {

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Get username from arguments (passed from OtpVerificationFragment)
        val username = arguments?.getString("username") ?: ""

        val etNewPassword = view.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = view.findViewById<EditText>(R.id.etConfirmPassword)
        val btnResetPassword = view.findViewById<Button>(R.id.btnResetPassword)
        val btnBackToLogin = view.findViewById<Button>(R.id.btnBackToLogin)

        btnResetPassword.setOnClickListener {

            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // Validate inputs
            if (newPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.resetPassword(
                username = username,
                newPassword = newPassword,
                onSuccess = {
                    Toast.makeText(
                        requireContext(),
                        "Password reset successfully! Please login with your new password",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(R.id.action_reset_password_to_login)
                },
                onError = { error ->
                    Toast.makeText(
                        requireContext(),
                        error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        btnBackToLogin.setOnClickListener {
            findNavController().popBackStack(R.id.loginFragment, false)
        }
    }
}