package com.jsac.sync.data.remote.api

import com.jsac.sync.data.remote.dto.LoginRequest
import com.jsac.sync.data.remote.dto.LoginResponse
import com.jsac.sync.data.remote.dto.RegisterRequest
import com.jsac.sync.data.remote.dto.RegisterResponse
import com.jsac.sync.data.remote.dto.VerifyOtpRequest
import com.jsac.sync.data.remote.dto.VerifyOtpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    // ✅ NEW: Reuses the same VerifyOtpRequest/Response DTOs used by the
    // forgot-password flow, since the OTP verification contract is identical.
    @POST("verify-registration-otp")
    suspend fun verifyRegistrationOtp(
        @Body request: VerifyOtpRequest
    ): Response<VerifyOtpResponse>

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}