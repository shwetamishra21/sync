package com.jsac.sync.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsac.sync.data.repository.ForgotPasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

/**
 * ✅ FIXED: Updated for OTP-based password reset
 * Changes:
 * 1. resetPassword() no longer requires reset_token
 * 2. Token verification is done via verify-otp endpoint instead
 * 3. OTP is verified before reaching this fragment
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

                    Log.d("ForgotPasswordViewModel", "✅ OTP sent to: $username")

                    onSuccess()

                } else {

                    Log.d("ForgotPasswordViewModel", "❌ Failed - Status: ${response.code()}")

                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        if (!errorBody.isNullOrEmpty()) {
                            val json = JSONObject(errorBody)
                            json.optString("message", "Email/username not found")
                        } else {
                            "Email/username not found"
                        }
                    } catch (e: Exception) {
                        Log.e("ForgotPasswordViewModel", "Error parsing error response: ${e.message}")
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
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        Log.d("ForgotPasswordViewModel", "🔄 Resetting password for: $username")

        viewModelScope.launch {

            try {

                val response = repository.resetPassword(username, newPassword)

                Log.d("ForgotPasswordViewModel", "📡 API Response Code: ${response.code()}")
                Log.d("ForgotPasswordViewModel", "📡 API Response Body: ${response.body()}")

                if (response.isSuccessful) {

                    Log.d("ForgotPasswordViewModel", "✅ Password reset successfully for: $username")

                    onSuccess()

                } else {

                    Log.d("ForgotPasswordViewModel", "❌ Reset failed - Status: ${response.code()}")

                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        if (!errorBody.isNullOrEmpty()) {
                            val json = JSONObject(errorBody)
                            json.optString("message", "Error resetting password")
                        } else {
                            "Error resetting password"
                        }
                    } catch (e: Exception) {
                        Log.e("ForgotPasswordViewModel", "Error parsing error response: ${e.message}")
                        "Error resetting password"
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