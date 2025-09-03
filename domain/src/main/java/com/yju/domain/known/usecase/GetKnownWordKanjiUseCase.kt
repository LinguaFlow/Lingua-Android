package com.yju.domain.known.usecase

import com.yju.domain.known.model.KnownWordKanjiModel
import com.yju.domain.known.repository.KnownWordKanjiRepository
import javax.inject.Inject

class GetKnownWordKanjiUseCase @Inject constructor(
    private val getAll: KnownWordKanjiRepository
) {
    suspend operator fun invoke(id: Long): KnownWordKanjiModel{
        return getAll.getKnownKanjiVocabulary(id)
    }
}