package com.jsac.sync.presentation.splash


import androidx.lifecycle.ViewModel
import com.jsac.sync.data.local.datastore.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    val isLoggedIn: Flow<Boolean> =
        sessionManager.isLoggedIn
}