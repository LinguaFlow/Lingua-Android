package com.yju.presentation.module


import com.yju.data.kanji.api.KanjiService
import com.yju.data.kanji.remote.KanjiRemoteDataSourceImpl
import com.yju.data.pdf.api.PdfService
import com.yju.data.pdf.remote.PdfRemoteDataSource
import com.yju.data.pdf.remote.PdfRemoteDataSourceImpl
import com.yju.domain.auth.util.SharedPreferenceUtil
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
    fun providePdfRemoteDataSource(
        pdfService: PdfService,
    ): PdfRemoteDataSource = PdfRemoteDataSourceImpl(pdfService)

    @Provides
    @Singleton
    fun provideKanjiRemoteDataSource(kanjiService: KanjiService) = KanjiRemoteDataSourceImpl(kanjiService)

}