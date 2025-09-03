package com.yju.presentation.module

import com.yju.domain.kanji.repository.KanjiLocalRepository
import com.yju.domain.kanji.usecase.SaveKanjiVocabularyUseCase
import com.yju.domain.pdf.repository.PdfRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideKanjiDao(kanjiRepository: KanjiLocalRepository) = SaveKanjiVocabularyUseCase(kanjiRepository)

}