package com.jsac.sync.presentation.splash

import androidx.lifecycle.ViewModel
import com.jsac.sync.data.local.datastore.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    sessionManager: SessionManager
) : ViewModel() {

    val isLoggedIn: Flow<Boolean> =

        sessionManager.token.map {

            !it.isNullOrEmpty()
        }
}