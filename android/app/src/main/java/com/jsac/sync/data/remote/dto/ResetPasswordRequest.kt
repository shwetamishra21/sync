package com.jsac.sync.data.remote.dto

/**
 * ✅ FIXED: Changed 'email' field to 'username'
 * Flask backend expects: data.get('username'), not data.get('email')
 *
 * This ensures consistency across the forgot password flow:
 * - User enters email/username on ForgotPasswordFragment
 * - Backend stores it as 'username' in memory
 * - ResetPasswordRequest must also use 'username' field
 */
data class ResetPasswordRequest(
    val username: String,
    val reset_token: String,
    val new_password: String
)