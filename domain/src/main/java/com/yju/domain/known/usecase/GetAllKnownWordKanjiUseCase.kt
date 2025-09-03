package com.yju.domain.known.usecase

import com.yju.domain.known.model.KnownWordKanjiModel
import com.yju.domain.known.repository.KnownWordKanjiRepository
import javax.inject.Inject

/**
 * 모든 PDF 목록을 가져오는 UseCase
 */
class GetAllKnownWordKanjiUseCase @Inject constructor(
    private val getAll: KnownWordKanjiRepository
) {
    suspend operator fun invoke(): List<KnownWordKanjiModel> {
        return getAll.getAllKnownKanjiVocabulary()
    }
}