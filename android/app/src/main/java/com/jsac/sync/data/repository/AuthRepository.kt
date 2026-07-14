package com.jsac.sync.data.repository

import com.jsac.sync.data.remote.api.AuthApi
import com.jsac.sync.data.remote.dto.LoginRequest
import com.jsac.sync.data.remote.dto.RegisterRequest
import com.jsac.sync.data.remote.dto.VerifyOtpRequest
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: AuthApi
) {

    suspend fun register(
        username: String,
        password: String
    ) = api.register(

        RegisterRequest(
            username,
            password
        )
    )

    // ✅ NEW: Verifies the OTP sent during registration
    suspend fun verifyRegistrationOtp(
        username: String,
        otp: String
    ) = api.verifyRegistrationOtp(
        VerifyOtpRequest(
            username,
            otp
        )
    )

    suspend fun login(
        username: String,
        password: String
    ) = api.login(

        LoginRequest(
            username,
            password
        )
    )
}