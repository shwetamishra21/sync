package com.jsac.sync.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsac.sync.data.local.datastore.SessionManager
import com.jsac.sync.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    fun register(
        username: String,
        password: String,
        onResult: (String) -> Unit
    ) {

        Log.d("AuthViewModel", "📝 Register attempt - Username: $username")

        viewModelScope.launch {

            try {

                val response =
                    repository.register(
                        username,
                        password
                    )

                if (response.isSuccessful) {

                    Log.d("AuthViewModel", "✅ Registration successful")

                    onResult(
                        response.body()?.message
                            ?: "Registration successful"
                    )

                } else {

                    Log.d("AuthViewModel", "❌ Registration failed - Status: ${response.code()}")

                    onResult(
                        "Registration failed"
                    )
                }

            } catch (e: Exception) {

                Log.e("AuthViewModel", "❌ Register exception: ${e.message}", e)

                onResult(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun login(
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        Log.d("AuthViewModel", "🔐 Login attempt - Username: $username")

        viewModelScope.launch {

            try {

                val response =
                    repository.login(
                        username,
                        password
                    )

                if (response.isSuccessful &&
                    response.body() != null
                ) {

                    val token =
                        response.body()!!.token

                    Log.d("AuthViewModel", "✅ Login successful - Token received (length: ${token.length})")

                    // ✅ FIXED: Now we wait for token to be saved before calling onSuccess()
                    // Previously saveToken() was async and returned immediately,
                    // causing a race condition where navigation happened before token was persisted
                    viewModelScope.launch {
                        sessionManager.saveToken(token)

                        Log.d("AuthViewModel", "💾 Token persisted to DataStore")

                        // Only call onSuccess() after token is actually saved
                        onSuccess()
                    }

                } else {

                    Log.d("AuthViewModel", "❌ Login failed - Status: ${response.code()}, Body: ${response.body()}")

                    onError(
                        "Invalid credentials"
                    )
                }

            } catch (e: Exception) {

                Log.e("AuthViewModel", "❌ Login exception: ${e.message}", e)

                onError(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun logout() {

        Log.d("AuthViewModel", "🚪 Logout initiated")

        viewModelScope.launch {

            sessionManager.clearSession()

            Log.d("AuthViewModel", "✅ Logout complete - Session cleared")
        }
    }
}