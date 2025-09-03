package com.yju.domain.kanji.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
@Parcelize
data class KanjiDetailModel(
    @SerialName("vocabulary_book_order")
    val vocabularyBookOrder: Int,

    @SerialName("kanji")
    val kanji: String,

    @SerialName("furigana")
    val furigana: String,

    @SerialName("means")
    val means: String,

    @SerialName("level")
    val level: String,

    @SerialName("page")
    val page: Int,
    ) : Parcelable