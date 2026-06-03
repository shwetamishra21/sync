package com.jsac.sync.data.remote.api

import com.jsac.sync.data.remote.dto.LoginRequest
import com.jsac.sync.data.remote.dto.LoginResponse
import com.jsac.sync.data.remote.dto.RegisterRequest
import com.jsac.sync.data.remote.dto.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}