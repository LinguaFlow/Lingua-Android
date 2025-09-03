package com.yju.data.known.remote

import com.yju.data.known.dao.KnownWordKanjiDao
import com.yju.data.known.entity.KnownWordKanji
import javax.inject.Inject

class KnownWordKanjiLocalDataSourceImpl @Inject constructor(
    private val knownKanjiDao: KnownWordKanjiDao
) : KnownWordKanjiLocalDataSource {
    override suspend fun insertKnownKanjiVocabulary(unknowKanjl: KnownWordKanji): Long {
        return knownKanjiDao.insertUnknownKanjiVocabulary(unknowKanjl)
    }

    override suspend fun getKnownKanjiKanjiVocabularyById(id: Long): KnownWordKanji {
        return knownKanjiDao.getUnknownKanjiKanjiVocabularyById(id)
    }

    override suspend fun deleteKnownKanjiKanjiVocabularyById(id: Long) {
        return knownKanjiDao.deleteKnownKanjiKanjiVocabularyById(id)
    }

    override suspend fun getAllKnownKanjiKanjiVocabulary(): List<KnownWordKanji> {
        return knownKanjiDao.getAllKnownKanjiKanjiVocabulary()
    }

    // 추가: 책 이름으로 최신 항목 조회
    override suspend fun getLatestKnownKanjiByBookName(bookName: String): KnownWordKanji? {
        return knownKanjiDao.getLatestUnknownKanjiByBookName(bookName)
    }

    // 추가: 책 이름으로 삭제
    override suspend fun deleteKnownKanjiByBookName(bookName: String) {
        knownKanjiDao.deleteKnownKanjiByBookName(bookName)
    }

    // 추가: 책별 최신 항목 조회
    override suspend fun getLatestKnownKanjiForEachBook(): List<KnownWordKanji> {
        return knownKanjiDao.getLatestKnownKanjiForEachBook()
    }

    override suspend fun deleteAllKnownKanji() {
        knownKanjiDao.deleteAllKnownKanji()
    }
}