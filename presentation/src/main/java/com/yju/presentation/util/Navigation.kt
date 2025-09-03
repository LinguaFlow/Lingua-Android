package com.yju.presentation.util

sealed class Navigation {
    data class ToPdfViewer(val bookId: String? = null) : Navigation()
    data class ToPdfChapter(val pdfId: Long) : Navigation()
    data class ToPdfWords(val pdfId: Long, val chapterTitle: String) : Navigation()
}
