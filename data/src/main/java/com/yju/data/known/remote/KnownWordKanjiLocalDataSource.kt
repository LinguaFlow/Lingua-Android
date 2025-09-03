package com.yju.data.known.remote

import com.yju.data.known.entity.KnownWordKanji

interface KnownWordKanjiLocalDataSource {
    suspend fun insertKnownKanjiVocabulary(kanji: KnownWordKanji): Long
    suspend fun getKnownKanjiKanjiVocabularyById(id: Long) : KnownWordKanji
    suspend fun deleteKnownKanjiKanjiVocabularyById(id: Long)
    suspend fun getAllKnownKanjiKanjiVocabulary(): List<KnownWordKanji>
    // 추가: 책 이름으로 최신 항목 조회
    suspend fun getLatestKnownKanjiByBookName(bookName: String): KnownWordKanji?
    // 추가: 책 이름으로 삭제
    suspend fun deleteKnownKanjiByBookName(bookName: String)
    // 추가: 책별 최신 항목 조회
    suspend fun getLatestKnownKanjiForEachBook(): List<KnownWordKanji>
    suspend fun deleteAllKnownKanji()

}