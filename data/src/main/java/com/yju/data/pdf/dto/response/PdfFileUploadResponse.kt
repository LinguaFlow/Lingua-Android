package com.yju.data.pdf.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PdfFileUploadResponse(
    @SerialName("book_name")
    val bookName: String,
    @SerialName("file_name")
    val word: List<PdfKanjiDetailResponse>
)