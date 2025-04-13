package com.yju.domain.pdf.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PdfUploadModel(
    @SerialName("pk")
    val id : Long,

    @SerialName("number")
    val number: Int,

    @SerialName("kanji")
    val kanji: String,

    @SerialName("furigana")
    val furigana: String,

    @SerialName("means")
    val means: String,

    @SerialName("level")
    val level: String
)