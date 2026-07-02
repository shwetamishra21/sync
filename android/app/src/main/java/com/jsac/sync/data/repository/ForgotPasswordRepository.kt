package com.jsac.sync.data.repository

import com.jsac.sync.data.remote.api.ForgotPasswordApi
import com.jsac.sync.data.remote.dto.ForgotPasswordRequest
import com.jsac.sync.data.remote.dto.ResetPasswordRequest
import com.jsac.sync.data.remote.dto.VerifyOtpRequest
import javax.inject.Inject

/**
 * ✅ FIXED: Updated for OTP-based password reset
 * - resetPassword() no longer requires reset_token
 * - verifyOtp() is called before resetPassword()
 */
class ForgotPasswordRepository @Inject constructor(
    private val api: ForgotPasswordApi
) {

    suspend fun requestPasswordReset(username: String) =
        api.requestPasswordReset(ForgotPasswordRequest(username))

    suspend fun verifyOtp(
        username: String,
        otp: String
    ) = api.verifyOtp(
        VerifyOtpRequest(
            username,
            otp
        )
    )

    suspend fun resetPassword(
        username: String,
        newPassword: String
    ) = api.resetPassword(
        ResetPasswordRequest(username, newPassword)
    )
}