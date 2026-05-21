package com.jsac.sync.data.remote.dto


data class ResetPasswordRequest(
    val email: String,
    val reset_token: String,
    val new_password: String
)