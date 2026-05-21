package com.jsac.sync.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsac.sync.data.repository.ForgotPasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val repository: ForgotPasswordRepository
) : ViewModel() {

    fun requestPasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        Log.d("ForgotPasswordViewModel", "🔐 Password reset requested for: $email")

        viewModelScope.launch {

            try {

                val response = repository.requestPasswordReset(email)

                if (response.isSuccessful) {

                    Log.d("ForgotPasswordViewModel", "✅ Reset link sent to email")

                    onSuccess()

                } else {

                    Log.d("ForgotPasswordViewModel", "❌ Failed - Status: ${response.code()}")

                    onError("Email not found or error occurred")
                }

            } catch (e: Exception) {

                Log.e("ForgotPasswordViewModel", "❌ Exception: ${e.message}", e)

                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun resetPassword(
        email: String,
        resetToken: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        Log.d("ForgotPasswordViewModel", "🔄 Resetting password for: $email")

        viewModelScope.launch {

            try {

                val response = repository.resetPassword(email, resetToken, newPassword)

                if (response.isSuccessful) {

                    Log.d("ForgotPasswordViewModel", "✅ Password reset successfully")

                    onSuccess()

                } else {

                    Log.d("ForgotPasswordViewModel", "❌ Reset failed - Status: ${response.code()}")

                    onError("Invalid token or error occurred")
                }

            } catch (e: Exception) {

                Log.e("ForgotPasswordViewModel", "❌ Exception: ${e.message}", e)

                onError(e.message ?: "Unknown error")
            }
        }
    }
}