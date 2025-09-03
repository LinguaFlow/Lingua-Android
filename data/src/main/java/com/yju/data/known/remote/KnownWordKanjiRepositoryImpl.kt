package com.yju.data.known.remote


import com.yju.data.known.entity.KnownWordKanji
import com.yju.data.known.mapper.toKanjiDetails
import com.yju.data.known.mapper.toKnownKanjiModel
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.domain.known.model.KnownWordKanjiModel
import com.yju.domain.known.repository.KnownWordKanjiRepository
import javax.inject.Inject

class KnownWordKanjiRepositoryImpl @Inject constructor(
    private val knownKanjiDao: KnownWordKanjiLocalDataSource
) : KnownWordKanjiRepository {
    override suspend fun insertKnownKanjiVocabulary(
        bookName: String,
        word: List<KanjiDetailModel>
    ): Long {
        val knownWord = mapOf("items" to word.map { it.toKanjiDetails() })
        val unknownKanji = KnownWordKanji(
            bookName = bookName,
            knownWord = knownWord
        )
        return knownKanjiDao.insertKnownKanjiVocabulary(unknownKanji)
    }

    override suspend fun deleteKnownKanjiVocabularyById(id: Long) {
        return knownKanjiDao.deleteKnownKanjiKanjiVocabularyById(id)
    }

    override suspend fun getKnownKanjiVocabulary(id: Long): KnownWordKanjiModel {
        val dao = knownKanjiDao.getKnownKanjiKanjiVocabularyById(id)
        return dao.toKnownKanjiModel()
    }

    override suspend fun getAllKnownKanjiVocabulary(): List<KnownWordKanjiModel> {
        val allUnknownKanjiKanjiVocabulary = knownKanjiDao.getAllKnownKanjiKanjiVocabulary()
        return allUnknownKanjiKanjiVocabulary.map { it.toKnownKanjiModel() }
    }

    // 추가: 책 이름으로 최신 항목 조회
    override suspend fun getLatestKnownKanjiByBookName(bookName: String): KnownWordKanjiModel? {
        val dao = knownKanjiDao.getLatestKnownKanjiByBookName(bookName) ?: return null
        return dao.toKnownKanjiModel()
    }

    // 추가: 책 이름으로 삭제
    override suspend fun deleteKnownKanjiByBookName(bookName: String) {
        knownKanjiDao.deleteKnownKanjiByBookName(bookName)
    }

    // 추가: 책별 최신 항목 조회
    override suspend fun getLatestKnownKanjiForEachBook(): List<KnownWordKanjiModel> {
        val result = knownKanjiDao.getLatestKnownKanjiForEachBook()
        return result.map { it.toKnownKanjiModel() }
    }

    override suspend fun deleteAllKanji() {
        knownKanjiDao.deleteAllKnownKanji()
    }
}