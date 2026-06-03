package com.jsac.sync.utils

import android.util.Patterns

object EmailValidator {

    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun getEmailErrorMessage(email: String): String? {
        return when {
            email.isEmpty() -> "Email cannot be empty"
            !isValidEmail(email) -> "Please enter a valid email address"
            else -> null
        }
    }
}