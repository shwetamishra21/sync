package com.jsac.sync.data.remote.api


import com.jsac.sync.data.remote.dto.ForgotPasswordRequest
import com.jsac.sync.data.remote.dto.ForgotPasswordResponse
import com.jsac.sync.data.remote.dto.ResetPasswordRequest
import com.jsac.sync.data.remote.dto.ResetPasswordResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ForgotPasswordApi {

    @POST("forgot-password")
    suspend fun requestPasswordReset(
        @Body request: ForgotPasswordRequest
    ): Response<ForgotPasswordResponse>

    @POST("reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ResetPasswordResponse>
}