package com.yju.domain.known.usecase

import com.yju.domain.kanji.repository.KanjiLocalRepository
import javax.inject.Inject

class UpdateKanjiTitleUseCase @Inject constructor(
    private val repository: KanjiLocalRepository
) {
    suspend operator fun invoke(id: Long, newTitle: String) {
        repository.updateKanjiTitle(id, newTitle)
    }
}