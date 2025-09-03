package com.yju.domain.known.usecase

import com.yju.domain.known.repository.KnownWordKanjiRepository
import javax.inject.Inject

class DeleteKnownWordKanjiByBookNameUseCase @Inject constructor(
    private val unknownKanjiRepository: KnownWordKanjiRepository
) {
    suspend fun deleteUnknownKanjiByBookName(bookName: String) {
        unknownKanjiRepository.deleteKnownKanjiByBookName(bookName)
    }
}