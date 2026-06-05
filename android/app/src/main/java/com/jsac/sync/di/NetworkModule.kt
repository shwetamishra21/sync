package com.jsac.sync.di

import com.jsac.sync.BuildConfig
import com.jsac.sync.data.remote.api.AuthApi
import com.jsac.sync.data.remote.api.FormApi
import com.jsac.sync.data.remote.api.ForgotPasswordApi
import com.jsac.sync.data.remote.api.HealthApi
import com.jsac.sync.data.remote.api.SubmissionApi
import com.jsac.sync.data.remote.interceptor.AuthInterceptor
import com.google.gson.Gson
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

    // Use BuildConfig.API_BASE_URL instead of hardcoded string
    // This allows different URLs for debug/release builds

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {

        val loggingInterceptor = HttpLoggingInterceptor { message ->
            println("🌐 HTTP LOG: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
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

    @Provides
    @Singleton
    fun provideFormApi(
        retrofit: Retrofit
    ): FormApi {

        return retrofit.create(FormApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSubmissionApi(
        retrofit: Retrofit
    ): SubmissionApi {

        return retrofit.create(SubmissionApi::class.java)
    }
}