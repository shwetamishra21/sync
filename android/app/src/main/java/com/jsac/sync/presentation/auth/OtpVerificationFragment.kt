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
 * ✅ FIXED: OTP Verification Fragment
 * User receives OTP via email and verifies it here
 * After verification, navigates to Reset Password screen
 */
@AndroidEntryPoint
class OtpVerificationFragment :
    Fragment(R.layout.fragment_otp_verification) {

    private val viewModel: OtpVerificationViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val username =
            requireArguments().getString("username") ?: ""

        val etOtp =
            view.findViewById<EditText>(R.id.etOtp)

        val btnVerify =
            view.findViewById<Button>(R.id.btnVerifyOtp)

        btnVerify.setOnClickListener {

            val otp =
                etOtp.text.toString().trim()

            if (otp.isBlank()) {

                Toast.makeText(
                    requireContext(),
                    "Enter OTP",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            viewModel.verifyOtp(

                username = username,
                otp = otp,

                onSuccess = {

                    Toast.makeText(
                        requireContext(),
                        "OTP verified successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    val bundle = Bundle().apply {
                        putString(
                            "username",
                            username
                        )
                    }

                    findNavController().navigate(
                        R.id.action_otpVerificationFragment_to_resetPasswordFragment,
                        bundle
                    )

                },

                onError = {

                    Toast.makeText(
                        requireContext(),
                        it,
                        Toast.LENGTH_SHORT
                    ).show()

                }

            )

        }

    }

}