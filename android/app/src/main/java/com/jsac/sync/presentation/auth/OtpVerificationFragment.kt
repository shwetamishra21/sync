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
 * ✅ REUSED: Same OTP verification screen/component powers two flows,
 * distinguished by the "flow" nav argument:
 *  - "forgot_password" (default) -> after verification, go to Reset Password
 *  - "register"                  -> after verification, registration is
 *                                    complete server-side, go straight to Login
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

        val username = requireArguments().getString("username") ?: ""
        val flow = requireArguments().getString("flow") ?: "forgot_password"

        val etOtp = view.findViewById<EditText>(R.id.etOtp)

        val btnVerify = view.findViewById<Button>(R.id.btnVerifyOtp)

        val btnResendOtp =
            view.findViewById<Button>(R.id.btnResendOtp)

        btnVerify.setOnClickListener {

            val otp = etOtp.text.toString().trim()

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
                flow = flow,

                onSuccess = {

                    Toast.makeText(
                        requireContext(),
                        if (flow == "register")
                            "Registration successful! Please login."
                        else
                            "OTP verified successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (flow == "register") {

                        // Registration is complete on the backend now,
                        // so send the user straight to Login.
                        findNavController().navigate(
                            R.id.action_otpVerificationFragment_to_login
                        )

                    } else {

                        val bundle = Bundle().apply {
                            putString("username", username)
                        }

                        findNavController().navigate(
                            R.id.action_otpVerificationFragment_to_resetPasswordFragment,
                            bundle
                        )
                    }

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

        // Optional resend button
        btnResendOtp.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "A new OTP has been requested.",
                Toast.LENGTH_SHORT
            ).show()

            // Replace this with your backend resend API when available
            // viewModel.requestOtp(username, flow)

        }
    }
}
