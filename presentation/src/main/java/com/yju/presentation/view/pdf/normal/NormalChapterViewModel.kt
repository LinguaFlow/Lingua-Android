package com.yju.presentation.view.pdf.normal

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yju.domain.kanji.model.KanjiModel
import com.yju.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class NormalChapterViewModel @Inject constructor() : BaseViewModel() {

    companion object {
        private const val TAG = "NormalChapterViewModel"
        private const val WORDS_PER_PAGE = 10
    }
    private val _chapters = MutableStateFlow<List<String>>(emptyList())
    val chapters: StateFlow<List<String>> = _chapters.asStateFlow()
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _wordCountsByPage = MutableLiveData<Map<Int, Int>>(emptyMap())
    val wordCountsByPage: LiveData<Map<Int, Int>> = _wordCountsByPage
    private var cachedPdfInfo: KanjiModel? = null
    private var cachedChapters: List<String> = emptyList()
    private var cachedWordCounts: Map<Int, Int> = emptyMap()

    fun setPdfInfo(pdfInfo: KanjiModel) {
        // 이미 같은 PDF면 스킵
        if (cachedPdfInfo?.id == pdfInfo.id &&
            cachedPdfInfo?.word?.size == pdfInfo.word.size) {
            Log.d(TAG, "동일한 PDF 정보, 처리 스킵")
            return
        }
        Log.d(TAG, "PDF 정보 설정: ${pdfInfo.bookName}, ${pdfInfo.word.size}개 단어")
        cachedPdfInfo = pdfInfo
        // 백그라운드에서 챕터 생성
        viewModelScope.launch(Dispatchers.Default) {
            generateChaptersFromInfo(pdfInfo)
        }
    }

    fun loadPdfInfo(pdfId: Long) {
        // 이미 로딩 중이면 스킵
        if (_isLoading.value == true) {
            Log.d(TAG, "이미 로딩 중, 스킵")
            return
        }

        // 캐시된 정보가 있고 같은 PDF면 재사용
        if (cachedPdfInfo?.id == pdfId) {
            Log.d(TAG, "캐시된 PDF 정보 재사용")
            _chapters.value = cachedChapters
            _wordCountsByPage.value = cachedWordCounts
            return
        }

        Log.d(TAG, "PDF 정보 로드 시작: ID $pdfId")
        _isLoading.value = true
    }

    fun regenerateChapters() {
        cachedPdfInfo?.let { pdfInfo ->
            Log.d(TAG, "캐시된 데이터로 챕터 재생성")
            viewModelScope.launch(Dispatchers.Default) {
                generateChaptersFromInfo(pdfInfo)
            }
        } ?: Log.w(TAG, "캐시된 PDF 정보가 없음")
    }

    private suspend fun generateChaptersFromInfo(pdfInfo: KanjiModel) = withContext(Dispatchers.Default) {
        try {
            val totalWords = pdfInfo.word.size
            Log.d(TAG, "서브페이지 생성 시작: ${totalWords}개 단어")

            if (totalWords == 0) {
                updateChapters(emptyList(), emptyMap())
                return@withContext
            }
            
            if (cachedPdfInfo?.word?.size == totalWords && cachedChapters.isNotEmpty()) {
                Log.d(TAG, "캐시된 챕터 재사용")
                withContext(Dispatchers.Main) {
                    _chapters.value = cachedChapters
                    _wordCountsByPage.value = cachedWordCounts
                }
                return@withContext
            }

            // 새 챕터 생성
            val chapters = mutableListOf<String>()
            val wordCounts = mutableMapOf<Int, Int>()

            var pageIndex = 0
            var remainingWords = totalWords

            while (remainingWords > 0) {
                val wordsInThisPage = minOf(WORDS_PER_PAGE, remainingWords)
                val subIndex = (pageIndex % WORDS_PER_PAGE) + 1
                val mainPage = (pageIndex / WORDS_PER_PAGE) + 1

                chapters.add("$mainPage:$subIndex")
                wordCounts[pageIndex] = wordsInThisPage

                remainingWords -= wordsInThisPage
                pageIndex++
            }

            Log.d(TAG, "서브페이지 제목 생성 완료: ${chapters.size}개")

            cachedChapters = chapters
            cachedWordCounts = wordCounts
            updateChapters(chapters, wordCounts)

        } catch (e: Exception) {
            Log.e(TAG, "챕터 생성 실패", e)
            updateChapters(emptyList(), emptyMap())
        }
    }

    private suspend fun updateChapters(
        chapters: List<String>,
        wordCounts: Map<Int, Int>
    ) = withContext(Dispatchers.Main) {
        _chapters.value = chapters
        _wordCountsByPage.value = wordCounts
        _isLoading.value = false
        Log.d(TAG, "페이지별 단어 수 매핑 완료: $wordCounts")
        Log.d(TAG, "서브페이지 생성 완료")
    }

    override fun onCleared() {
        super.onCleared()
        cachedPdfInfo = null
        cachedChapters = emptyList()
        cachedWordCounts = emptyMap()
        Log.d(TAG, "ViewModel 정리 완료")
    }
}