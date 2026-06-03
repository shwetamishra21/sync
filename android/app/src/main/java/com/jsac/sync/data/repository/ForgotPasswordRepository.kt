package com.jsac.sync.data.repository

import com.jsac.sync.data.remote.api.ForgotPasswordApi
import com.jsac.sync.data.remote.dto.ForgotPasswordRequest
import com.jsac.sync.data.remote.dto.ResetPasswordRequest
import javax.inject.Inject

class ForgotPasswordRepository @Inject constructor(
    private val api: ForgotPasswordApi
) {

    suspend fun requestPasswordReset(email: String) =
        api.requestPasswordReset(ForgotPasswordRequest(email))

    suspend fun resetPassword(
        email: String,
        resetToken: String,
        newPassword: String
    ) = api.resetPassword(
        ResetPasswordRequest(email, resetToken, newPassword)
    )
}