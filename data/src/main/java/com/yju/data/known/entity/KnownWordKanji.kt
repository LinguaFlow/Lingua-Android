package com.yju.data.known.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.yju.data.kanji.util.DateTimeConverters
import com.yju.data.kanji.util.KanjiDataConverters

import java.time.LocalDateTime

@Entity(tableName = "known_kanji")
@TypeConverters(DateTimeConverters::class , KanjiDataConverters::class)
class KnownWordKanji(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "book_name")
    val bookName: String,

    @ColumnInfo(name = "unknown_word")
    val knownWord: Map<String, Any>,

    @ColumnInfo(name = "createdAt", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updatedAt", defaultValue = "CURRENT_TIMESTAMP")
    val updatedAt: LocalDateTime? = null,

    @ColumnInfo(name = "deletedAt")
    val deletedAt: LocalDateTime? = null
)