package com.jsac.sync.data.remote.interceptor

import android.util.Log
import com.jsac.sync.data.local.datastore.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor to add JWT token to all API requests
 *
 * ✅ FIXED: Now includes detailed logging to debug token issues
 *
 * Flow:
 * 1. Get token from SessionManager (DataStore)
 * 2. Log token state for debugging
 * 3. If token exists, add Authorization header
 * 4. Proceed with request
 * 5. Log success or warning
 *
 * Debugging:
 * - If you see "❌ Token is null or empty", user needs to login again
 * - If you see "✅ Token found", token was attached to request
 * - If you see no logs, interceptor might not be in OkHttpClient
 */
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(
        chain: Interceptor.Chain
    ): Response {

        val token = runBlocking {
            sessionManager.token.first()
        }

        // ============================================
        // ✅ FIXED: Detailed token logging for debugging
        // ============================================

        if (token.isNullOrEmpty()) {
            Log.e("AuthInterceptor", "❌ CRITICAL: Token is null or empty!")
            Log.e("AuthInterceptor", "⚠️ This request will fail with 401 Unauthorized")
            Log.e("AuthInterceptor", "📍 Request: ${chain.request().method} ${chain.request().url}")
            Log.e("AuthInterceptor", "💡 Solution: User needs to login again to get a valid token")
        } else {
            Log.d("AuthInterceptor", "✅ Token found (length: ${token.length} chars)")
            Log.d("AuthInterceptor", "📍 Request: ${chain.request().method} ${chain.request().url}")
            Log.d("AuthInterceptor", "🔐 Token will be attached to request")
        }

        val request = chain.request()
            .newBuilder()

        if (!token.isNullOrEmpty()) {

            request.addHeader(
                "Authorization",
                "Bearer $token"
            )

            Log.d("AuthInterceptor", "✅ Authorization header added: Bearer ${token.take(20)}...")

        } else {

            Log.e("AuthInterceptor", "❌ Proceeding WITHOUT authorization header")
            Log.e("AuthInterceptor", "⚠️ Backend will likely reject this request with 401")
        }

        // Make the actual request
        val response = chain.proceed(
            request.build()
        )

        // ✅ FIXED: Log response status for debugging
        Log.d("AuthInterceptor", "📡 Response: ${response.code} ${response.message}")

        if (response.code == 401) {
            Log.e("AuthInterceptor", "❌ Got 401 Unauthorized - token might be invalid or expired")
        }

        return response
    }
}