package com.yju.presentation.modul

import com.yju.domain.pdf.model.PdfUploadModel
import com.yju.domain.pdf.repository.PdfRepository
import com.yju.domain.pdf.usecase.PdfJoinUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @qProvides
    @Singleton
    fun provideUploadPdf(pdfRepository: PdfRepository) = PdfJoinUseCase(pdfRepository)
}