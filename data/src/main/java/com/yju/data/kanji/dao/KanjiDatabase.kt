package com.yju.data.kanji.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yju.data.kanji.entity.Kanji
import com.yju.data.known.dao.KnownWordKanjiDao
import com.yju.data.known.entity.KnownWordKanji

import com.yju.data.kanji.util.DateTimeConverters
import com.yju.data.kanji.util.KanjiDataConverters

@Database(entities = [Kanji::class , KnownWordKanji::class], version = 2, exportSchema = false)
@TypeConverters(
    DateTimeConverters::class,
    KanjiDataConverters::class
)
abstract class KanjiDatabase : RoomDatabase() {
    abstract fun kanjiDao(): KanjiDao
    abstract fun unknownKanjiDao(): KnownWordKanjiDao
}