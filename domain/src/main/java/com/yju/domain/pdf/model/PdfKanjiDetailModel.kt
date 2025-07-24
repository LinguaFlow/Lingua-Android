package com.yju.domain.pdf.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PdfKanjiDetailModel(
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
    val page: Int
)