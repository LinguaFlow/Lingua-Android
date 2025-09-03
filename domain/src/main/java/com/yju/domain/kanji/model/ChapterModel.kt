package com.yju.domain.kanji.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ChapterModel(
    val id: Long,

    @SerialName("title")
    val title: String,

    @SerialName("word_count")
    val wordCount: Int,

    @SerialName("page_range")
    val pageRange: String
) : Parcelable