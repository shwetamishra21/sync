
package com.jsac.sync.data.remote.dto

data class VerifyOtpRequest(
    val username: String,
    val otp: String
)