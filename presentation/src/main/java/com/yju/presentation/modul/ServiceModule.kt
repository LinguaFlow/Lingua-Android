package com.yju.presentation.modul



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
}