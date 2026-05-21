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
class ResetPasswordFragment : Fragment(R.layout.fragment_reset_password) {

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val email = arguments?.getString("email") ?: ""

        val etResetToken = view.findViewById<EditText>(R.id.etResetToken)
        val etNewPassword = view.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = view.findViewById<EditText>(R.id.etConfirmPassword)
        val btnResetPassword = view.findViewById<Button>(R.id.btnResetPassword)
        val btnBackToLogin = view.findViewById<Button>(R.id.btnBackToLogin)

        btnResetPassword.setOnClickListener {

            val resetToken = etResetToken.text.toString().trim()
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // Validate inputs
            if (resetToken.isEmpty()) {
                Toast.makeText(requireContext(), "Enter reset token from your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                email,
                resetToken,
                newPassword,
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