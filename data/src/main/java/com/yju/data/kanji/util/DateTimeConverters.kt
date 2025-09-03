package com.yju.data.kanji.util

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateTimeConverters {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDataTime(value: LocalDateTime?): String? = value?.format(formatter)


    @TypeConverter
    fun toLocalDataTime(value: String?): LocalDateTime? =
        value?.let { LocalDateTime.parse(it, formatter) }

}