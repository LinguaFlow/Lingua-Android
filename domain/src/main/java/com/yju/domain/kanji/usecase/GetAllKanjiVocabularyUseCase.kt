package com.yju.domain.kanji.usecase

import com.yju.domain.kanji.model.KanjiModel
import com.yju.domain.kanji.repository.KanjiLocalRepository
import javax.inject.Inject

/**
 * 모든 PDF 목록을 가져오는 UseCase
 */
class GetAllKanjiVocabularyUseCase @Inject constructor(
    private val getAll: KanjiLocalRepository
) {
    suspend operator fun invoke(): List<KanjiModel> {
        return getAll.getAllKanjiVocabulary()
    }
}