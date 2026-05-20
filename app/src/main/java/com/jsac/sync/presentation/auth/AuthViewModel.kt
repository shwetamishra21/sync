package com.jsac.sync.presentation.auth

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

        viewModelScope.launch {

            try {

                val response =
                    repository.register(
                        username,
                        password
                    )

                if (response.isSuccessful) {

                    onResult(
                        response.body()?.message
                            ?: "Registration successful"
                    )

                } else {

                    onResult(
                        "Registration failed"
                    )
                }

            } catch (e: Exception) {

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

                    sessionManager.saveToken(
                        token
                    )

                    onSuccess()

                } else {

                    onError(
                        "Invalid credentials"
                    )
                }

            } catch (e: Exception) {

                onError(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun logout() {

        viewModelScope.launch {

            sessionManager.clearSession()
        }
    }
}