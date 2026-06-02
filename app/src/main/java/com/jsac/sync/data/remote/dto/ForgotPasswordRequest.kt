package com.jsac.sync.data.remote.dto

/**
 * ✅ FIXED: Changed field from 'email' to 'username'
 * Flask backend expects: data.get('username')
 *
 * This was the PRIMARY ROOT CAUSE of the "Email not found or error occurred" error
 */
data class ForgotPasswordRequest(
    val username: String
)