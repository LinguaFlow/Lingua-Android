package com.yju.domain.kanji.usecase


import com.yju.domain.kanji.repository.KanjiLocalRepository
import com.yju.domain.pdf.model.PdfKanjiDetailModel

import javax.inject.Inject

class SaveKanjiVocabularyUseCase @Inject constructor(
    private val kanjiRepository: KanjiLocalRepository
) {
    suspend operator fun invoke(bookName: String, word: List<PdfKanjiDetailModel>): Long {
        return kanjiRepository.saveKanjiVocabulary(bookName , word)
    }
}
