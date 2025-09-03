package com.yju.data.kanji.remote

import com.yju.data.kanji.dao.KanjiDao
import com.yju.data.kanji.entity.Kanji
import com.yju.domain.kanji.model.KanjiModel
import javax.inject.Inject

class KanjiLocalDataSourceImpl @Inject constructor(
    private val kanjiDao: KanjiDao
) : KanjiLocalDataSource {

    override suspend fun insertKanjiVocabulary(kanji: Kanji): Long {
        return kanjiDao.insertKanjiVocabulary(kanji)
    }

    override suspend fun getKanjiVocabularyById(id: Long): Kanji {
        return kanjiDao.getKanjiVocabularyById(id)
    }

    override suspend fun deleteKanjiVocabularyById(id: Long) {
        kanjiDao.deleteKanjiVocabularyById(id)
    }

    override suspend fun getAllKanjiVocabulary(): List<Kanji> {
        return kanjiDao.getAllKanjiVocabulary()
    }
}