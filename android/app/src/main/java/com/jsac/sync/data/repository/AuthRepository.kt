package com.jsac.sync.data.repository

import com.jsac.sync.data.remote.api.AuthApi
import com.jsac.sync.data.remote.dto.LoginRequest
import com.jsac.sync.data.remote.dto.RegisterRequest
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