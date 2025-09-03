package com.yju.data.kanji.remote


import android.util.Log
import com.yju.data.kanji.mapper.toKanji
import com.yju.data.kanji.mapper.toKanjiModel
import com.yju.domain.kanji.model.KanjiModel
import com.yju.domain.kanji.repository.KanjiLocalRepository
import com.yju.domain.pdf.model.PdfKanjiDetailModel
import com.yju.domain.pdf.model.PdfModel
import timber.log.Timber
import javax.inject.Inject


class KanjiLocalRepositoryImpl @Inject constructor(
    private val kanjiLocalDataSource: KanjiLocalDataSource
) : KanjiLocalRepository {
    override suspend fun getAllKanjiVocabulary(): List<KanjiModel> {
        val kanjiList = kanjiLocalDataSource.getAllKanjiVocabulary()
        return kanjiList.map { it.toKanjiModel() }
    }

    override suspend fun saveKanjiVocabulary(
        bookName: String,
        wordList: List<PdfKanjiDetailModel>
    ): Long {
        try {
            Timber.tag("KanjiDebug")
                .d("한자 저장 시작 - bookName: $bookName, wordList size: ${wordList.size}")
            val kanjiVocabulary = PdfModel(bookName, wordList)
            Timber.tag("KanjiDebug").d("PdfModel 생성 완료")
            val result = kanjiLocalDataSource.insertKanjiVocabulary(kanjiVocabulary.toKanji())
            Timber.tag("KanjiDebug").d("DB 저장 완료 - ID: $result")
            return result
        } catch (e: Exception) {
            Timber.tag("KanjiDebug").e(e, "한자 단어장 저장 실패: ${e.message}")
            throw e
        }
    }
    override suspend fun getKanjiVocabulary(id: Long): KanjiModel {
        val kanji = kanjiLocalDataSource.getKanjiVocabularyById(id)
        return kanji.toKanjiModel()
    }

    override suspend fun deleteKanjiVocabularyById(id: Long) {
        kanjiLocalDataSource.deleteKanjiVocabularyById(id)
    }

    override suspend fun updateKanjiTitle(id: Long, newTitle: String) {
        TODO("Not yet implemented")
    }
}
