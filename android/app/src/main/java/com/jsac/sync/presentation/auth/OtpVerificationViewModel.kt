package com.jsac.sync.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsac.sync.data.repository.AuthRepository
import com.jsac.sync.data.repository.ForgotPasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

/**
 * ✅ REUSED: OTP Verification ViewModel
 * Shared by both the Forgot Password flow and the Registration flow,
 * so the OTP entry/validation UX and error handling logic lives in one place.
 *
 * Dispatches to the correct backend endpoint based on [flow]:
 *  - "forgot_password" -> ForgotPasswordRepository.verifyOtp()      -> POST /verify-otp
 *  - "register"        -> AuthRepository.verifyRegistrationOtp()    -> POST /verify-registration-otp
 */
@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    private val forgotPasswordRepository: ForgotPasswordRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    fun verifyOtp(
        username: String,
        otp: String,
        flow: String = "forgot_password",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        Log.d("OtpVerificationViewModel", "🔐 Verifying OTP for: $username (flow=$flow)")

        viewModelScope.launch {

            try {

                val response = if (flow == "register") {
                    authRepository.verifyRegistrationOtp(username, otp)
                } else {
                    forgotPasswordRepository.verifyOtp(username, otp)
                }

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
