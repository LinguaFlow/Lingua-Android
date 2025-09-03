package com.yju.data.kanji.remote

import com.yju.data.kanji.entity.Kanji
import com.yju.domain.kanji.model.KanjiModel


interface KanjiLocalDataSource {
    suspend fun insertKanjiVocabulary(kanji: Kanji): Long
    suspend fun getKanjiVocabularyById(id: Long) : Kanji
    suspend fun deleteKanjiVocabularyById(id: Long)
    suspend fun getAllKanjiVocabulary(): List<Kanji>

}