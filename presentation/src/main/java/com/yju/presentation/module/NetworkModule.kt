package com.yju.presentation.module

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.yju.data.auth.util.HeaderInterceptor
import com.yju.domain.auth.util.AuthInterceptor

import com.yju.domain.auth.util.SharedPreferenceUtil
import com.yju.domain.util.WebSocketManager
import com.yju.presentation.util.CustomCallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import okhttp3.Authenticator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideAuthInterceptor(prefs: SharedPreferenceUtil): Authenticator {
        return AuthInterceptor(prefs)
    }

    @Singleton
    @Provides
    fun provideHeaderInterceptor(prefs: SharedPreferenceUtil): HeaderInterceptor {
        return HeaderInterceptor(prefs)
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        headerInterceptor: HeaderInterceptor
    ) = run {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideJsonConfiguration(): Json {
        return Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    @ExperimentalSerializationApi
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .client(okHttpClient)
//            .baseUrl("https://linguaflow.store/")
            .baseUrl("http://192.168.1.100:8080/")
            .addCallAdapterFactory(CustomCallAdapterFactory())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
    @Provides
    @Singleton
    fun provideWebSocketManager(
        okHttpClient: OkHttpClient
    ): WebSocketManager {
        return WebSocketManager(
            okHttpClient = okHttpClient,
//            baseUrl = "https://linguaflow.store/",
            baseUrl = "http://192.168.1.100:8080/"

        )
    }
}