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
 * ✅ FIXED: OTP Verification ViewModel
 * Handles OTP validation from email
 */
@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    private val repository: ForgotPasswordRepository
) : ViewModel() {

    fun verifyOtp(
        username: String,
        otp: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        Log.d("OtpVerificationViewModel", "🔐 Verifying OTP for: $username")

        viewModelScope.launch {

            try {

                val response = repository.verifyOtp(username, otp)

                Log.d("OtpVerificationViewModel", "📡 API Response Code: ${response.code()}")
                Log.d("OtpVerificationViewModel", "📡 API Response Body: ${response.body()}")

                if (response.isSuccessful) {

                    Log.d("OtpVerificationViewModel", "✅ OTP verified successfully for: $username")
                    onSuccess()

                } else {

                    Log.d("OtpVerificationViewModel", "❌ OTP verification failed - Status: ${response.code()}")

                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        if (!errorBody.isNullOrEmpty()) {
                            val json = JSONObject(errorBody)
                            json.optString("message", "Invalid OTP")
                        } else {
                            "Invalid OTP"
                        }
                    } catch (e: Exception) {
                        Log.e("OtpVerificationViewModel", "Error parsing error response: ${e.message}")
                        "Invalid OTP"
                    }

                    onError(errorMessage)
                }

            } catch (e: Exception) {

                Log.e("OtpVerificationViewModel", "❌ Exception: ${e.message}", e)
                e.printStackTrace()

                onError(e.message ?: "Unknown error occurred")
            }
        }
    }
}