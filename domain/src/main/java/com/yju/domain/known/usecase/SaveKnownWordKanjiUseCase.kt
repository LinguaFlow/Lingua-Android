package com.yju.domain.known.usecase


import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.domain.known.repository.KnownWordKanjiRepository

import javax.inject.Inject

class SaveKnownWordKanjiUseCase @Inject constructor(
    private val kanjiRepository: KnownWordKanjiRepository
) {
    suspend operator fun invoke(bookName: String, word: List<KanjiDetailModel>): Long {
        return kanjiRepository.insertKnownKanjiVocabulary(bookName , word)
    }
}
