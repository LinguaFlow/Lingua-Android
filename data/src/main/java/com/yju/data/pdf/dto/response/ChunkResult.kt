package com.yju.data.pdf.dto.response

data class ChunkResult(
    val body: PdfFileUploadResponse, // 50개 리스트
    val received: Int,
    val expected: Int,
    val done: Boolean
)