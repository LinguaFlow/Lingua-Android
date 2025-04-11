package com.yju.presentation.module

import com.yju.data.pdf.remote.PdfRemoteDataSourceImpl
import com.yju.data.pdf.remote.PdfRepositoryImpl
import com.yju.domain.pdf.repository.PdfRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePdfRepository(pdfRemoteDataSource: PdfRemoteDataSourceImpl): PdfRepository {
        return PdfRepositoryImpl(pdfRemoteDataSource)
    }
}