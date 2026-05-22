package com.jsac.sync.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsac.sync.data.repository.ForgotPasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ✅ FIXED: Multiple improvements:
 * 1. Changed parameter names from 'email' to 'username'
 * 2. Added better error logging to debug issues
 * 3. Added response body logging to see actual backend errors
 * 4. Fixed error message extraction from response
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val repository: ForgotPasswordRepository
) : ViewModel() {

    fun requestPasswordReset(
        username: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        Log.d("ForgotPasswordViewModel", "🔐 Password reset requested for: $username")

        viewModelScope.launch {

            try {

                val response = repository.requestPasswordReset(username)

                Log.d("ForgotPasswordViewModel", "📡 API Response Code: ${response.code()}")
                Log.d("ForgotPasswordViewModel", "📡 API Response Body: ${response.body()}")
                Log.d("ForgotPasswordViewModel", "📡 API Error Body: ${response.errorBody()?.string()}")

                if (response.isSuccessful) {

                    Log.d("ForgotPasswordViewModel", "✅ Reset link sent to username: $username")

                    onSuccess()

                } else {

                    Log.d("ForgotPasswordViewModel", "❌ Failed - Status: ${response.code()}")

                    // Try to extract error message from response
                    val errorMessage = try {
                        response.errorBody()?.string() ?: "Email/username not found"
                    } catch (e: Exception) {
                        "Email/username not found"
                    }

                    onError(errorMessage)
                }

            } catch (e: Exception) {

                Log.e("ForgotPasswordViewModel", "❌ Exception: ${e.message}", e)
                e.printStackTrace()

                onError(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun resetPassword(
        username: String,
        resetToken: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        Log.d("ForgotPasswordViewModel", "🔄 Resetting password for: $username")

        viewModelScope.launch {

            try {

                val response = repository.resetPassword(username, resetToken, newPassword)

                Log.d("ForgotPasswordViewModel", "📡 API Response Code: ${response.code()}")
                Log.d("ForgotPasswordViewModel", "📡 API Response Body: ${response.body()}")

                if (response.isSuccessful) {

                    Log.d("ForgotPasswordViewModel", "✅ Password reset successfully for: $username")

                    onSuccess()

                } else {

                    Log.d("ForgotPasswordViewModel", "❌ Reset failed - Status: ${response.code()}")

                    val errorMessage = try {
                        response.errorBody()?.string() ?: "Invalid token or error occurred"
                    } catch (e: Exception) {
                        "Invalid token or error occurred"
                    }

                    onError(errorMessage)
                }

            } catch (e: Exception) {

                Log.e("ForgotPasswordViewModel", "❌ Exception: ${e.message}", e)
                e.printStackTrace()

                onError(e.message ?: "Unknown error occurred")
            }
        }
    }
}