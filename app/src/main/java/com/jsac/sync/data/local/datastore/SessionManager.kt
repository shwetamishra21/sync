package com.jsac.sync.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

        private val USER_TOKEN =
            stringPreferencesKey("user_token")
    }

    suspend fun saveToken(
        token: String
    ) {

        context.dataStore.edit { preferences ->

            preferences[USER_TOKEN] = token
        }
    }

    suspend fun clearSession() {

        context.dataStore.edit { preferences ->

            preferences.clear()
        }
    }

    val token: Flow<String?> =

        context.dataStore.data.map { preferences ->

            preferences[USER_TOKEN]
        }
}