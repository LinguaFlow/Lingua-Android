package com.yju.domain.known.usecase

import com.yju.domain.known.model.TranslationExamplesModel
import com.yju.domain.known.repository.KanjiRemoteRepository
import com.yju.domain.known.repository.KnownWordKanjiRepository
import javax.inject.Inject

class CreateExampleUseCase @Inject constructor(
    private val kanjiRemoteRepository: KanjiRemoteRepository
) {
    suspend fun getTranslationExamples(
        word: String,
        level: String
    ): Result<TranslationExamplesModel> {
        return kanjiRemoteRepository.getTranslationExamples(word, level)
    }
}