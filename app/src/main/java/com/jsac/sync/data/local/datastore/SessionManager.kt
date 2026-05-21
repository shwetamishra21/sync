package com.jsac.sync.data.local.datastore

import android.content.Context
import android.util.Log
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

        Log.d("SessionManager", "💾 Saving token of length: ${token.length}")

        context.dataStore.edit { preferences ->

            preferences[USER_TOKEN] = token

            Log.d("SessionManager", "✅ Token saved successfully")
        }
    }

    suspend fun clearSession() {

        Log.d("SessionManager", "🗑️ Clearing all session data")

        context.dataStore.edit { preferences ->

            preferences.clear()

            Log.d("SessionManager", "✅ Session cleared successfully")
        }
    }

    val token: Flow<String?> =

        context.dataStore.data.map { preferences ->

            val token = preferences[USER_TOKEN]

            if (token != null) {
                Log.d("SessionManager", "📍 Token retrieved from DataStore (length: ${token.length})")
            } else {
                Log.d("SessionManager", "📍 No token in DataStore (fresh start or cleared)")
            }

            token
        }

    // Debug helper - call this to check current token state
    suspend fun debugPrintToken() {
        context.dataStore.data.collect { preferences ->
            val token = preferences[USER_TOKEN]
            Log.d("SessionManager", "🔍 DEBUG - Current token: ${token ?: "NULL (Will trigger login flow)"}")
        }
    }
}