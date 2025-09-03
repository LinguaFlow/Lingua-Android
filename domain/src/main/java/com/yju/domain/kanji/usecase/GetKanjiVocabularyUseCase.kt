package com.yju.domain.kanji.usecase

import com.yju.domain.kanji.model.KanjiModel
import com.yju.domain.kanji.repository.KanjiLocalRepository
import javax.inject.Inject

class GetKanjiVocabularyUseCase @Inject constructor(
    private val getAll: KanjiLocalRepository
) {
    suspend operator fun invoke(id: Long): KanjiModel{
        return getAll.getKanjiVocabulary(id)
    }
}