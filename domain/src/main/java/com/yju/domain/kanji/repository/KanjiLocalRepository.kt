package com.yju.domain.kanji.repository

import com.yju.domain.kanji.model.KanjiModel
import com.yju.domain.pdf.model.PdfKanjiDetailModel


interface KanjiLocalRepository {
    suspend fun getAllKanjiVocabulary(): List<KanjiModel>
    suspend fun getKanjiVocabulary(id: Long) : KanjiModel
    suspend fun saveKanjiVocabulary(bookName: String, word: List<PdfKanjiDetailModel>): Long
    suspend fun deleteKanjiVocabularyById(id: Long)
    suspend fun updateKanjiTitle(id: Long, newTitle: String)

}