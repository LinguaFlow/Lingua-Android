package com.yju.data.pdf.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PdfKanjiDetailResponse (
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