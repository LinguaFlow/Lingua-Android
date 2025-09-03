package com.yju.data.kanji.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yju.data.kanji.entity.Kanji

@Dao
interface KanjiDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKanjiVocabulary(kanji: Kanji): Long

    @Transaction
    @Query("SELECT * FROM kanji WHERE id = :id")
    suspend fun getKanjiVocabularyById(id: Long): Kanji

    @Transaction
    @Query("DELETE FROM kanji WHERE id = :id")
    suspend fun deleteKanjiVocabularyById(id: Long)

    @Transaction
    @Query("select * from kanji where deletedAt is null")
    suspend fun getAllKanjiVocabulary(): List<Kanji>
}