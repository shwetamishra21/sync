package com.jsac.sync.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "sync_preferences"
)

class SessionManager(
    private val context: Context
) {

    companion object {

        private val IS_LOGGED_IN =
            booleanPreferencesKey("is_logged_in")
    }

    suspend fun setLoggedIn(
        isLoggedIn: Boolean
    ) {

        context.dataStore.edit { preferences ->

            preferences[IS_LOGGED_IN] = isLoggedIn
        }
    }

    val isLoggedIn: Flow<Boolean> =

        context.dataStore.data.map { preferences ->

            preferences[IS_LOGGED_IN] ?: false
        }
}