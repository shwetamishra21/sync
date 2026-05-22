package com.jsac.sync.di

import com.jsac.sync.data.remote.api.AuthApi
import com.jsac.sync.data.remote.api.ForgotPasswordApi
import com.jsac.sync.data.remote.api.HealthApi
import com.jsac.sync.data.remote.interceptor.AuthInterceptor
import com.jsac.sync.data.remote.interceptor.LoggingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // 🎯 YOUR BACKEND IP - From VS Code Flask terminal: 192.168.87.80
    // For Android Emulator on same machine, use: 10.0.2.2 (this is the host machine IP from emulator perspective)
    private const val BASE_URL = "http://192.168.87.80:5000/"

    // Alternative base URLs:
    // private const val BASE_URL = "http://10.0.2.2:5000/"         // Only for emulator on same machine
    // private const val BASE_URL = "http://localhost:5000/"        // Only on same machine
    // private const val BASE_URL = "http://YOUR_IP:5000/"          // Replace YOUR_IP with your machine IP

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {

        // ✅ FIXED: Added explicit HTTP logging for debugging
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            // Log both request and response bodies for debugging
            println("🌐 HTTP LOG: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY  // Log full request/response body
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)  // ✅ Added logging interceptor
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideHealthApi(
        retrofit: Retrofit
    ): HealthApi {

        return retrofit.create(HealthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthApi(
        retrofit: Retrofit
    ): AuthApi {

        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideForgotPasswordApi(
        retrofit: Retrofit
    ): ForgotPasswordApi {

        return retrofit.create(ForgotPasswordApi::class.java)
    }
}