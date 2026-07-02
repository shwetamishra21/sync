package com.jsac.sync.data.remote.dto

/**
 * ✅ FIXED: Removed 'reset_token' field
 * OTP verification is done separately via verify-otp endpoint.
 * This request only needs username and new_password.
 *
 * Flow:
 * 1. User enters email on ForgotPasswordFragment
 * 2. Backend sends OTP
 * 3. User enters OTP on OtpVerificationFragment
 * 4. verify-otp endpoint validates OTP
 * 5. User enters new password on ResetPasswordFragment
 * 6. reset-password endpoint updates password (OTP already verified)
 */
data class ResetPasswordRequest(
    val username: String,
    val new_password: String
)