package com.jsac.sync.data.remote.dto

/**
 * ✅ FIXED: Removed 'reset_token' field
 * Backend no longer returns the token directly.
 * OTP is sent via email instead.
 */
data class ForgotPasswordResponse(
    val message: String
)