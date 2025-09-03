package com.yju.presentation.view.pdf.known

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.MutableEventFlow
import com.yju.presentation.util.asEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KnownWordChapterViewModel @Inject constructor() : BaseViewModel() {

    companion object {
        private const val TAG = "KnownWordChapterViewModel"
        private const val PAGE_SIZE = 10
    }
    private val _knownChapters = MutableLiveData<List<String>>(emptyList())
    val knownChapters: LiveData<List<String>> = _knownChapters
    private val _navigateToKnownWord = MutableEventFlow<Pair<Long, String>>()
    val navigateToKnownWord: EventFlow<Pair<Long, String>> = _navigateToKnownWord.asEventFlow()
    private var currentPdfId: Long = 0L
    private var cachedWords: List<KanjiDetailModel> = emptyList()
    private var cachedChapters: List<String> = emptyList()

    /**
     * ✅ PDF ID 설정
     */
    fun setPdfId(pdfId: Long) {
        if (currentPdfId != pdfId) {
            currentPdfId = pdfId
            // PDF가 변경되면 캐시 초기화
            clearCache()
            Log.d(TAG, "PDF ID 설정: $pdfId")
        }
    }

    fun updateKnownWords(words: List<KanjiDetailModel>) {
        // 동일한 데이터면 스킵
        if (words.size == cachedWords.size &&
            words.zip(cachedWords).all { (new, old) -> new.kanji == old.kanji }
        ) {
            Log.d(TAG, "동일한 단어 목록, 업데이트 스킵")
            return
        }

        cachedWords = words
        Log.d(TAG, "아는 단어 업데이트: ${words.size}개")

        // 챕터 생성
        generateChapters(words.size)
    }

    /**
     * ✅ 챕터 생성 - 간소화
     */
    private fun generateChapters(totalWords: Int) {
        val chapters = if (totalWords <= 0) {
            emptyList()
        } else {
            // 최소 1개 페이지는 생성
            val pageCount = ((totalWords - 1) / PAGE_SIZE + 1).coerceAtLeast(1)
            List(pageCount) { index -> "K:${index + 1}" }
        }

        // 캐시 업데이트
        cachedChapters = chapters

        // UI 업데이트
        _knownChapters.value = chapters

        Log.d(TAG, "챕터 생성 완료: ${chapters.size}개 페이지")
    }

    fun getKnownWordsByChapter(chapter: String): List<KanjiDetailModel> {
        if (cachedWords.isEmpty()) return emptyList()

        // "K:n" 형식 파싱
        val pageNum = chapter.removePrefix("K:").toIntOrNull() ?: return emptyList()

        val startIndex = (pageNum - 1) * PAGE_SIZE
        if (startIndex >= cachedWords.size) {
            Log.w(TAG, "페이지 범위 초과: $chapter")
            return emptyList()
        }

        val endIndex = minOf(startIndex + PAGE_SIZE, cachedWords.size)
        return cachedWords.subList(startIndex, endIndex)
    }
    fun getWordCountForChapter(chapter: String): Int {
        return getKnownWordsByChapter(chapter).size
    }
    fun onClickKnownChapter(pdfId: Long, chapter: String) {
        viewModelScope.launch {
            Log.d(TAG, "챕터 클릭: $chapter (PDF ID: $pdfId)")
            _navigateToKnownWord.emit(pdfId to chapter)
        }
    }

    private fun clearCache() {
        cachedWords = emptyList()
        cachedChapters = emptyList()
        _knownChapters.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        clearCache()
        Log.d(TAG, "ViewModel 정리 완료")
    }
}