package com.yju.domain.kanji.repository

import com.yju.domain.kanji.model.KanjiEntityModel
import kotlinx.coroutines.flow.Flow


interface KanjiRepository {
    suspend fun findAllKanji() : Result<List<KanjiEntityModel>>
}