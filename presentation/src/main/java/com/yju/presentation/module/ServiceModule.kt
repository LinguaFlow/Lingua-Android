package com.yju.presentation.module


import com.yju.data.auth.api.AuthService
import com.yju.data.kanji.api.KanjiService
import com.yju.data.pdf.api.PdfService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    @Provides
    @Singleton
    fun providePdfService(retrofit: Retrofit): PdfService {
        return retrofit.create(PdfService::class.java)
    }

    @Provides
    @Singleton
    fun provideKanjiService(retrofit: Retrofit): KanjiService {
        return retrofit.create(KanjiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }
}