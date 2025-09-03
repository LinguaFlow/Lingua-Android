package com.yju.data.pdf.mapper

import com.yju.data.pdf.dto.response.PdfFileUploadResponse
import com.yju.data.pdf.dto.response.PdfKanjiDetailResponse
import com.yju.domain.pdf.model.PdfKanjiDetailModel
import com.yju.domain.pdf.model.PdfModel

/* 서버 응답 → 도메인 PdfModel */
fun PdfFileUploadResponse.toPdfModel(): PdfModel {
    return PdfModel(
        bookName = this.bookName,
        word = this.word.map { it.toPdfKanjiDetailModel() }
    )
}

fun PdfKanjiDetailResponse.toPdfKanjiDetailModel(): PdfKanjiDetailModel {
    return PdfKanjiDetailModel(
        vocabularyBookOrder = this.vocabularyBookOrder,
        kanji = this.kanji,
        furigana = this.furigana,
        means = this.means,
        level = this.level,
        page = this.page
    )
}