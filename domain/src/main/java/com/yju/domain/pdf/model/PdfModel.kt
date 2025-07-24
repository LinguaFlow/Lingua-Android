package com.yju.domain.pdf.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PdfModel(
    @SerialName("book_name")
    val bookName: String,
    @SerialName("file_name")
    val word: List<PdfKanjiDetailModel>
)
