package com.yju.presentation.module


import com.yju.data.pdf.api.PdfService
import com.yju.data.pdf.remote.PdfRemoteDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun providePdfRemoteDataSource(pdfService: PdfService) = PdfRemoteDataSourceImpl(pdfService)
}