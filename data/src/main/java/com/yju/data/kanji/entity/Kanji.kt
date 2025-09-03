package com.yju.data.kanji.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.yju.data.kanji.util.DateTimeConverters
import com.yju.data.kanji.util.KanjiDataConverters
import java.time.LocalDateTime

@Entity(tableName = "kanji")
@TypeConverters(DateTimeConverters::class , KanjiDataConverters::class)
data class Kanji(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "book_name")
    val bookName: String,

    @ColumnInfo(name = "word")
    val word: Map<String, Any>,

    @ColumnInfo(name = "createdAt", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updatedAt", defaultValue = "CURRENT_TIMESTAMP")
    val updatedAt: LocalDateTime? = null,

    @ColumnInfo(name = "deletedAt")
    val deletedAt: LocalDateTime? = null
)