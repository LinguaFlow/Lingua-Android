package com.yju.domain.known.repository

import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.domain.known.model.KnownWordKanjiModel

interface KnownWordKanjiRepository {
    suspend fun insertKnownKanjiVocabulary(bookName: String, word: List<KanjiDetailModel>): Long

    suspend fun deleteKnownKanjiVocabularyById(id: Long)
    suspend fun getKnownKanjiVocabulary(id: Long): KnownWordKanjiModel

    suspend fun deleteKnownKanjiByBookName(bookName: String)
    suspend fun getLatestKnownKanjiByBookName(bookName: String): KnownWordKanjiModel?

    suspend fun getLatestKnownKanjiForEachBook(): List<KnownWordKanjiModel>
    suspend fun getAllKnownKanjiVocabulary(): List<KnownWordKanjiModel>

    suspend fun deleteAllKanji()
}