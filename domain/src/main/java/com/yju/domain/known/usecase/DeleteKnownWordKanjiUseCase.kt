package com.yju.domain.known.usecase

import com.yju.domain.known.repository.KnownWordKanjiRepository
import javax.inject.Inject

class DeleteKnownWordKanjiUseCase @Inject constructor(
    private val unknownKanjiRepository: KnownWordKanjiRepository
) {
    suspend fun deleteUnknownKanjiUseCase(id: Long) {
        unknownKanjiRepository.deleteKnownKanjiVocabularyById(id)
    }
}