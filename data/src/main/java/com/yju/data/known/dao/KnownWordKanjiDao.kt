package com.yju.data.known.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yju.data.known.entity.KnownWordKanji

@Dao
interface KnownWordKanjiDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertUnknownKanjiVocabulary(entity: KnownWordKanji): Long        // 한 PDF의 Unknown 세트 저장

    @Query("SELECT * FROM known_kanji WHERE id = :id")
    suspend fun getUnknownKanjiKanjiVocabularyById(id: Long): KnownWordKanji

    @Query("SELECT * FROM known_kanji WHERE book_name = :bookName ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestUnknownKanjiByBookName(bookName: String): KnownWordKanji?

    @Query("DELETE FROM known_kanji WHERE id = :id")
    suspend fun deleteKnownKanjiKanjiVocabularyById(id: Long)

    @Query("DELETE FROM known_kanji")
    suspend fun deleteAllKnownKanji()

    // 새로 추가: 책 이름으로 삭제 (같은 책의 모든 이전 버전 삭제)
    @Query("DELETE FROM known_kanji WHERE book_name = :bookName")
    suspend fun deleteKnownKanjiByBookName(bookName: String)

    @Query("SELECT * FROM known_kanji WHERE deletedAt IS NULL")
    suspend fun getAllKnownKanjiKanjiVocabulary(): List<KnownWordKanji>

    // 새로 추가: 모든 고유 책 이름 가져오기
    @Query("SELECT DISTINCT book_name FROM known_kanji WHERE deletedAt IS NULL")
    suspend fun getAllUniqueBookNames(): List<String>

    // 새로 추가: 책 이름별로 가장 최근 항목 가져오기
    @Query("SELECT uk.* FROM known_kanji uk INNER JOIN (SELECT book_name, MAX(createdAt) as maxCreatedAt FROM known_kanji GROUP BY book_name) t ON uk.book_name = t.book_name AND uk.createdAt = t.maxCreatedAt WHERE uk.deletedAt IS NULL")
    suspend fun getLatestKnownKanjiForEachBook(): List<KnownWordKanji>
}