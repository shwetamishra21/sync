package com.jsac.sync.data.remote.dto


data class ForgotPasswordResponse(
    val message: String,
    val reset_token: String?  // Token sent to user's email
)