package com.yju.domain.kanji.usecase.save

import com.yju.domain.kanji.repository.KanjiRepository
import javax.inject.Inject

class KanjiJoinUseCase @Inject constructor(
    private val kanjiRepository: KanjiRepository
) {
}