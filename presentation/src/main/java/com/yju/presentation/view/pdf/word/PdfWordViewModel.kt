package com.yju.presentation.view.pdf.word

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.domain.kanji.usecase.GetKanjiVocabularyUseCase
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.MutableEventFlow
import com.yju.presentation.util.asEventFlow
import com.yju.presentation.view.pdf.chapter.PdfChapterViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * PDF 단어 목록 화면의 ViewModel
 * 기존 방식 유지하면서 필요한 부분만 개선
 */
@HiltViewModel
class PdfWordViewModel @Inject constructor(
    private val getKanjiUseCase: GetKanjiVocabularyUseCase
) : BaseViewModel() {

    companion object {
        private const val TAG = "PdfWordViewModel"
        private const val PAGE_SIZE = 10
    }
    private val _words = MutableLiveData<List<KanjiDetailModel>>(emptyList())
    val words: LiveData<List<KanjiDetailModel>> = _words

    private val _chapterTitle = MutableLiveData("")
    val chapterTitle: LiveData<String> = _chapterTitle

    private val _back = MutableEventFlow<Boolean>()
    val back = _back.asEventFlow()

    private val _navigateToQuiz = MutableEventFlow<KanjiDetailModel>()
    val navigateToQuiz = _navigateToQuiz.asEventFlow()

    private var currentPdfId: Long = 0L
    private var isKnownChapter = false
    private var chapterViewModel: PdfChapterViewModel? = null

    fun onClickBack() = viewModelScope.launch {
        _back.emit(true)
    }

    fun onWordClick(word: KanjiDetailModel) = viewModelScope.launch {
        _navigateToQuiz.emit(word)
    }

    fun updateWordList(newWords: List<KanjiDetailModel>) {
        Log.d(TAG, "단어 목록 직접 업데이트: ${newWords.size}개 단어")
        _words.value = newWords
    }

    fun refreshWordList() = viewModelScope.launch {
        Log.d(TAG, "단어 목록 새로고침 (숨김 상태 반영)")
        val currentChapter = chapterTitle.value ?: return@launch

        if (!isKnownChapter && chapterViewModel != null) {
            loadChapterWords(currentPdfId, currentChapter, isKnownChapter, chapterViewModel)
        }
    }

    /**
     * 챕터 단어 로드 진입점 (기존 방식 유지)
     */
    fun loadChapterWords(
        pdfId: Long,
        chapterTitle: String,
        isKnown: Boolean = false,
        chapterViewModel: PdfChapterViewModel? = null
    ) {
        // 상태 업데이트
        currentPdfId = pdfId
        isKnownChapter = isKnown
        _chapterTitle.value = chapterTitle
        this.chapterViewModel = chapterViewModel

        // 챕터 타입에 따라 처리
        if (isKnown) {
            loadKnownChapterWords(pdfId, chapterTitle, chapterViewModel)
        } else {
            loadNormalChapterWords(pdfId, chapterTitle)
        }
    }

    /**
     * 일반 챕터 단어 로드
     */
    private fun loadNormalChapterWords(pdfId: Long, chapterTitle: String) {
        // 챕터 타입별 로딩 로직 분기
        when {
            // 새로운 형식: "1:2" (페이지:서브인덱스)
            Regex("(\\d+):(\\d+)").find(chapterTitle)?.let { match ->
                val (pageStr, subIndexStr) = match.destructured
                val pageNum = pageStr.toIntOrNull() ?: 1
                val subIndex = subIndexStr.toIntOrNull() ?: 1
                loadWordsByPageAndSubIndex(pageNum, subIndex, pdfId)
                true
            } == true -> { /* 패턴 매칭됨 */ }

            // 기존 형식: "Chapter 1 ~ 10"
            Regex("Chapter (\\d+) ~ (\\d+)").find(chapterTitle)?.let { match ->
                val (startStr, endStr) = match.destructured
                val start = startStr.toIntOrNull() ?: 1
                val end = endStr.toIntOrNull() ?: 10
                loadWordsByRange(start, end, pdfId)
                true
            } == true -> { /* 패턴 매칭됨 */ }

            // 페이지 형식: "Page 1" 처리
            Regex("Page (\\d+)").find(chapterTitle)?.let { match ->
                val pageStr = match.groupValues[1]
                val page = pageStr.toIntOrNull() ?: 1
                loadWordsByPage(page, pdfId)
                true
            } == true -> { /* 패턴 매칭됨 */ }
        }
    }

    /**
     * 페이지 번호와 서브인덱스로 단어 로드
     * 예: "1:2" 형식 (1페이지의 두 번째 10개 단어 묶음)
     */
    private fun loadWordsByPageAndSubIndex(pageNum: Int, subIndex: Int, pdfId: Long) = viewModelScope.launch {
        try {
            Log.d(TAG, "페이지/서브인덱스로 단어 로드: 페이지 $pageNum, 서브인덱스 $subIndex")

            // PDF 모델 로드
            val model = withContext(Dispatchers.IO) {
                getKanjiUseCase(pdfId)
            }

            // 해당 페이지의 단어들 필터링
            val pageWords = model.word.filter { it.page == pageNum }

            // 서브인덱스에 맞는 단어 슬라이스 계산
            val startIndex = (subIndex - 1) * PAGE_SIZE

            // 유효성 검사
            if (startIndex >= pageWords.size) {
                Log.e(TAG, "유효하지 않은 서브인덱스: 페이지 $pageNum 의 단어 수는 ${pageWords.size}, 요청된 서브인덱스는 $subIndex")
                _words.value = emptyList()
                return@launch
            }

            // 단어 슬라이스 추출
            val endIndex = minOf(startIndex + PAGE_SIZE, pageWords.size)
            val slicedWords = pageWords.subList(startIndex, endIndex)

            Log.d(TAG, "페이지 $pageNum, 서브인덱스 $subIndex 에서 로드된 단어: ${slicedWords.size}개")

            // 숨김 처리된 단어 필터링 (기존 방식 유지)
            val visibleWords = chapterViewModel?.filterVisibleWords(pdfId, slicedWords) ?: slicedWords
            _words.value = visibleWords
        } catch (e: Exception) {
            handleLoadError("페이지/서브인덱스 단어 로드 실패", e)
        }
    }

    /**
     * 페이지 번호로 단어 로드
     */
    private fun loadWordsByPage(pageNum: Int, pdfId: Long) = viewModelScope.launch {
        try {
            Log.d(TAG, "페이지 번호로 단어 로드: 페이지 $pageNum")

            // PDF 모델 로드
            val model = withContext(Dispatchers.IO) {
                getKanjiUseCase(pdfId)
            }

            // 페이지 번호로 단어 필터링
            val pageWords = model.word.filter { it.page == pageNum }
            Log.d(TAG, "페이지 $pageNum 에서 ${pageWords.size}개 단어 로드됨")

            // 숨김 처리된 단어 필터링
            val visibleWords = chapterViewModel?.filterVisibleWords(pdfId, pageWords) ?: pageWords
            _words.value = visibleWords
        } catch (e: Exception) {
            handleLoadError("페이지 단어 로드 실패", e)
        }
    }

    /**
     * 범위 내 단어 로드
     */
    private fun loadWordsByRange(start: Int, end: Int, pdfId: Long) = viewModelScope.launch {
        try {
            // PDF 모델 로드
            val model = withContext(Dispatchers.IO) {
                getKanjiUseCase(pdfId)
            }

            // 인덱스 슬라이싱으로 단어 필터링
            val filteredWords = model.word.drop(start - 1).take(end - start + 1)

            // 숨김 처리된 단어 필터링
            val visibleWords = chapterViewModel?.filterVisibleWords(pdfId, filteredWords) ?: filteredWords
            _words.value = visibleWords

            Log.d(TAG, "범위 $start-$end 에서 ${visibleWords.size}개 단어 로드됨")
        } catch (e: Exception) {
            handleLoadError("단어 로드 실패", e)
        }
    }

    /**
     * 모르는 단어 챕터 로드 (기존 방식 유지)
     */
    private fun loadKnownChapterWords(
        pdfId: Long,
        chapterTitle: String,
        chapterViewModel: PdfChapterViewModel?
    ) {
        if (chapterViewModel == null) {
            Log.d("모르는 단어 정보를 불러올 수 없습니다", "chapterViewModel이 null입니다")
            return
        }

        val allUnknownWords = chapterViewModel.getKnownWords(pdfId)
        Log.d(TAG, "전체 모르는 단어 수: ${allUnknownWords.size}")

        if (allUnknownWords.isEmpty()) {
            _words.value = emptyList()
            return
        }

        when {
            chapterTitle == "K:All" -> {
                Log.d(TAG, "모든 모르는 단어 표시: ${allUnknownWords.size}개")
                _words.value = allUnknownWords
            }

            chapterTitle.startsWith("K:") -> processKnownChapterFormat(chapterTitle, allUnknownWords)

            chapterTitle.startsWith("Unknown ") -> processLegacyKnownFormat(chapterTitle, allUnknownWords)
        }
    }

    private fun processKnownChapterFormat(chapterTitle: String, allWords: List<KanjiDetailModel>) {
        val pattern = Regex("K:(\\d+)(?::(\\d+))?")
        val match = pattern.find(chapterTitle)

        if (match != null) {
            val pageNum = match.groupValues[1].toIntOrNull() ?: 1

            val startIndex = (pageNum - 1) * 10

            if (startIndex >= allWords.size) {
                Log.d(TAG, "요청된 페이지 K:$pageNum 는 범위를 벗어납니다. 총 단어 수: ${allWords.size}")

                val lastPageIndex = ((allWords.size - 1) / 10) * 10

                val lastPageEndIndex = allWords.size

                Log.d(TAG, "대신 마지막 페이지의 단어(${lastPageIndex+1}-${lastPageEndIndex}) 반환")
                _words.value = allWords.subList(lastPageIndex, lastPageEndIndex)
                return
            }

            // 정상적인 범위 처리
            val endIndex = minOf(startIndex + 10, allWords.size)
            val selectedWords = allWords.subList(startIndex, endIndex)

            Log.d(TAG, "K:$pageNum 형식으로 단어 ${startIndex+1}-${endIndex} 로드됨: ${selectedWords.size}개")
            _words.value = selectedWords
        } else {
            Log.d(TAG, "유효하지 않은 K 형식 챕터: $chapterTitle")
            // 기본값으로 첫 페이지 보여주기
            if (allWords.isNotEmpty()) {
                val endIndex = minOf(10, allWords.size)
                _words.value = allWords.subList(0, endIndex)
            } else {
                _words.value = emptyList()
            }
        }
    }

    /**
     * "Unknown N ~ M" 형식 처리
     */
    private fun processLegacyKnownFormat(chapterTitle: String, allWords: List<KanjiDetailModel>) {
        val pattern = Regex("Unknown (\\d+) ~ (\\d+)")
        val match = pattern.find(chapterTitle)

        if (match != null) {
            val start = match.groupValues[1].toIntOrNull() ?: 1
            val end = match.groupValues[2].toIntOrNull() ?: 10

            val startIndex = start - 1

            if (startIndex < 0 || startIndex >= allWords.size) {
                Log.d(TAG, "범위 $start-$end 는 유효하지 않은 범위입니다 (전체 단어 수: ${allWords.size})")
                _words.value = emptyList()
                return
            }

            val endIndex = minOf(end, allWords.size)
            val selectedWords = allWords.subList(startIndex, endIndex)

            Log.d(TAG, "범위 $start-$end 에서 ${selectedWords.size}개 단어 로드됨")
            _words.value = selectedWords
        } else {
            Log.d("유효하지 않은 Unknown 형식 챕터", "잘못된 Unknown 형식 챕터: $chapterTitle")

            baseEvent(UiEvent.Toast.Normal("단어 불러오기 실패"))
        }
    }

    private fun handleLoadError(errorPrefix: String, e: Exception) {
        Log.e(TAG, "$errorPrefix: ${e.message}", e)
        baseEvent(UiEvent.Toast.Normal("단어 불러오기 실패: ${e.message}"))
        _words.value = emptyList()
    }
}