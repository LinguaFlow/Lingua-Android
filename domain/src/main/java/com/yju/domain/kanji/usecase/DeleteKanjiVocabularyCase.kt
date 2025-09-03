package com.yju.domain.kanji.usecase

import com.yju.domain.kanji.repository.KanjiLocalRepository
import javax.inject.Inject

class DeleteKanjiVocabularyCase @Inject constructor(
    private val kanjiRepository: KanjiLocalRepository
) {
    suspend operator fun invoke(id: Long) {
        return kanjiRepository.deleteKanjiVocabularyById(id)
    }
}