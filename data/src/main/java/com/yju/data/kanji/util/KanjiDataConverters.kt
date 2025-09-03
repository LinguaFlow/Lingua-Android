package com.yju.data.kanji.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object KanjiDataConverters {

    private val gson = Gson()

    @TypeConverter
    fun fromKanjiDataMap(value: Map<String, Any>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toKanjiDataMap(value: String?): Map<String, Any>? {
        if (value == null) return null
        val mapType = object : TypeToken<Map<String, List<KanjiDetails>>>() {}.type
        val specificMap = gson.fromJson<Map<String, List<KanjiDetails>>>(value, mapType)
        return specificMap as Map<String, Any>
    }
}