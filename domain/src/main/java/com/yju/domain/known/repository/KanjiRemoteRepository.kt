package com.yju.domain.known.repository

import com.yju.domain.known.model.TranslationExamplesModel

interface KanjiRemoteRepository {
    suspend fun getTranslationExamples(word: String, level: String): Result<TranslationExamplesModel>
}