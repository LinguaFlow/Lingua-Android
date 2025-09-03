package com.yju.presentation.view.pdf.chapter

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yju.data.pdf.util.PdfDataUtil
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.domain.kanji.model.KanjiModel
import com.yju.domain.kanji.usecase.GetKanjiVocabularyUseCase
import com.yju.domain.known.usecase.DeleteKnownWordKanjiByBookNameUseCase
import com.yju.domain.known.usecase.GetLatestKnownWordKanjiByBookNameUseCase
import com.yju.domain.known.usecase.SaveKnownWordKanjiUseCase
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.MutableEventFlow
import com.yju.presentation.util.asEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class PdfChapterViewModel @Inject constructor(
    private val getKanjiUseCase: GetKanjiVocabularyUseCase,
    private val saveKnownWordKanjiUseCase: SaveKnownWordKanjiUseCase,
    private val getKnownWordKanjiByBookNameUseCase: GetLatestKnownWordKanjiByBookNameUseCase,
    private val deleteKnownWordKanjiByBookNameUseCase: DeleteKnownWordKanjiByBookNameUseCase,
    @ApplicationContext private val context: Context
) : BaseViewModel() {

    companion object {
        private const val TAG = "PdfChapterViewModel"
    }
    private val _onClickBack = MutableEventFlow<Boolean>()
    val onClickBack: EventFlow<Boolean> = _onClickBack.asEventFlow()

    private val _onClickChapter = MutableEventFlow<Pair<Long, String>>()
    val onClickChapter: EventFlow<Pair<Long, String>> = _onClickChapter.asEventFlow()

    private val _onClickKnownChapter = MutableEventFlow<Pair<Long, String>>()
    val onClickKnownChapter: EventFlow<Pair<Long, String>> = _onClickKnownChapter.asEventFlow()

    private val _refreshKnownChapters = MutableEventFlow<Long>()
    val refreshKnownChapters = _refreshKnownChapters.asEventFlow()

    private val _refreshNormalChapters = MutableEventFlow<Long>()
    val refreshNormalChapters = _refreshNormalChapters.asEventFlow()

    private val _wordStatusChanged = MutableEventFlow<Pair<Long, List<KanjiDetailModel>>>()
    val wordStatusChanged = _wordStatusChanged.asEventFlow()

    private val _pdfInfo = MutableLiveData<KanjiModel?>()
    val pdfInfo: LiveData<KanjiModel?> = _pdfInfo

    // ✅ 최소한의 메모리 캐시만 유지
    private var currentPdfId: Long = 0L
    private var currentTabPosition = -1
    private val loadingMutex = Mutex()
    private val pdfInfoCache = mutableMapOf<Long, KanjiModel>()

    // ✅ PdfDataUtil 인스턴스 생성
    private fun getPdfDataUtil(): PdfDataUtil {
        return PdfDataUtil(context)
    }

    fun setCurrentPdfId(pdfId: Long) {
        // 이전 PDF와 다른 경우 이전 데이터 정리
        if (currentPdfId != 0L && currentPdfId != pdfId) {
            clearPdfData(currentPdfId)
        }
        currentPdfId = pdfId
        Log.d(TAG, "PDF ID 변경: $currentPdfId → $pdfId")
    }

    fun clearPdfData(pdfId: Long) {
        val pdfDataUtil = getPdfDataUtil()
        pdfDataUtil.clearPdfData(pdfId)
        pdfInfoCache.remove(pdfId)
        Log.d(TAG, "PDF $pdfId 데이터 정리 완료")
    }


    fun onClickBack() = viewModelScope.launch {
        _onClickBack.emit(true)
    }

    fun onClickChapter(pdfId: Long, chapter: String) = viewModelScope.launch {
        Log.d(TAG, "일반 챕터 선택: $pdfId / $chapter")
        _onClickChapter.emit(pdfId to chapter)
    }

    fun onClickKnownChapter(pdfId: Long, chapter: String) = viewModelScope.launch {
        Log.d(TAG, "아는단어 챕터 선택: $pdfId / $chapter")
        _onClickKnownChapter.emit(pdfId to chapter)
    }
    fun setCurrentTabPosition(position: Int) {
        currentTabPosition = position
    }

    fun loadPdfInfo(pdfId: Long, loadKnown: Boolean = true) = viewModelScope.launch {
        loadingMutex.withLock {
            try {
                // 캐시 확인
                val cachedInfo = pdfInfoCache[pdfId]
                if (cachedInfo != null) {
                    Log.d(TAG, "캐시된 PDF 정보 사용")
                    _pdfInfo.value = cachedInfo

                    // 아는 단어도 로드
                    if (loadKnown && getKnownWords(pdfId).isEmpty()) {
                        loadKnownWordsFromRepository(pdfId)
                    }
                    return@withLock
                }

                // 새로 로드
                val pdfInfo = withContext(Dispatchers.IO) {
                    getKanjiUseCase(pdfId)
                }

                Log.d(TAG, "PDF 정보 로드 완료: ${pdfInfo.bookName}")
                _pdfInfo.value = pdfInfo
                pdfInfoCache[pdfId] = pdfInfo

                // 초기화
                withContext(Dispatchers.Default) {
                    initializeHiddenStatus(pdfId)
                    if (loadKnown) {
                        loadKnownWordsFromRepository(pdfId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "PDF 정보 로드 실패", e)
                baseEvent(UiEvent.Toast.Normal("PDF 정보 로드 실패: ${e.message}"))
            }
        }
    }


    private suspend fun loadKnownWordsFromRepository(pdfId: Long) {
        try {
            val pdfDataUtil = getPdfDataUtil()

            // 자동 로드 비활성화 체크
            if (pdfDataUtil.isAutoLoadDisabled(pdfId)) {
                Log.d(TAG, "아는 단어 자동 로드가 비활성화됨: PDF ID $pdfId")
                return
            }

            // PDF 정보 확인
            val pdfInfo = _pdfInfo.value ?: run {
                Log.d(TAG, "PDF 정보가 없습니다. ID: $pdfId")
                return
            }

            val bookName = pdfInfo.bookName
            Log.d(TAG, "책 이름으로 저장소에서 아는 단어 로드 시도: $bookName")

            // 백그라운드에서 데이터 로드
            val knownKanjiModel = withContext(Dispatchers.IO) {
                getKnownWordKanjiByBookNameUseCase(bookName)
            }

            // 단어 처리
            if (knownKanjiModel != null && knownKanjiModel.word.isNotEmpty()) {
                val words = knownKanjiModel.word
                Log.d(TAG, "저장소에서 불러온 단어: ${words.size}개")

                // ✅ PdfDataUtil로 저장
                pdfDataUtil.saveKnownWords(pdfId, words)
                updateHiddenStatus(pdfId, words, true)
                pdfDataUtil.setHiddenStatusInitialized(pdfId, true)
                generateKnownChapters(words.size, pdfId)

                // UI 업데이트
                _refreshKnownChapters.emit(0)
                baseEvent(UiEvent.Toast.Normal("${words.size}개의 아는 단어를 로드했습니다"))

                // 로드 완료 상태 저장
                pdfDataUtil.setKnownWordsLoaded(pdfId, true)
            } else {
                Log.d(TAG, "저장소에서 불러올 단어가 없거나 모델이 null입니다")
                pdfDataUtil.setKnownWordsLoaded(pdfId, true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "아는 단어 로드 중 오류: ${e.message}", e)
        }
    }

    private suspend fun initializeHiddenStatus(pdfId: Long) {
        val pdfDataUtil = getPdfDataUtil()
        if (pdfDataUtil.isHiddenStatusInitialized(pdfId)) return

        val knownWords = loadKnownWordsFromRepositoryWithoutEffect(pdfId)
        if (knownWords.isNotEmpty()) {
            Log.d(TAG, "앱 시작 시 숨김 상태 초기화: ${knownWords.size}개 단어")
            updateHiddenStatus(pdfId, knownWords, true)
            pdfDataUtil.setHiddenStatusInitialized(pdfId, true)
        }
    }

    private suspend fun loadKnownWordsFromRepositoryWithoutEffect(pdfId: Long): List<KanjiDetailModel> {
        try {
            val pdfInfo = _pdfInfo.value ?: return emptyList()
            val bookName = pdfInfo.bookName

            val knownKanjiModel = withContext(Dispatchers.IO) {
                getKnownWordKanjiByBookNameUseCase(bookName)
            }

            if (knownKanjiModel != null && knownKanjiModel.word.isNotEmpty()) {
                val words = knownKanjiModel.word
                val pdfDataUtil = getPdfDataUtil()

                withContext(Dispatchers.Main) {
                    pdfDataUtil.saveKnownWords(pdfId, words)
                    generateKnownChapters(words.size, pdfId)
                    pdfDataUtil.setKnownWordsLoaded(pdfId, true)
                }

                return words
            }
        } catch (e: Exception) {
            Log.e(TAG, "아는 단어 조용히 로드 중 오류: ${e.message}", e)
        }
        return emptyList()
    }

    // ==================== 단어 상태 관리 (PdfDataUtil 사용) ====================
    fun updateHiddenStatus(pdfId: Long, words: List<KanjiDetailModel>, isHidden: Boolean) {
        val pdfDataUtil = getPdfDataUtil()
        val currentHidden = pdfDataUtil.getHiddenWords(pdfId).toMutableSet()

        words.forEach { word ->
            val wordKey = "${word.vocabularyBookOrder}:${word.kanji}"
            if (isHidden) {
                currentHidden.add(wordKey)
                Log.d(TAG, "단어 숨김 처리: $wordKey")
            } else {
                currentHidden.remove(wordKey)
                Log.d(TAG, "단어 숨김 해제: $wordKey")
            }
        }

        pdfDataUtil.saveHiddenWords(pdfId, currentHidden)
        Log.d(TAG, "숨김 상태 업데이트: ${currentHidden.size}개 숨김, PDF ID: $pdfId")
    }


    fun filterVisibleWords(pdfId: Long, words: List<KanjiDetailModel>): List<KanjiDetailModel> {
        val pdfDataUtil = getPdfDataUtil()
        val hiddenWords = pdfDataUtil.getHiddenWords(pdfId)
        return words.filterNot { word ->
            val wordKey = "${word.vocabularyBookOrder}:${word.kanji}"
            hiddenWords.contains(wordKey)
        }
    }

    fun getKnownWords(pdfId: Long): List<KanjiDetailModel> {
        val pdfDataUtil = getPdfDataUtil()
        val words = pdfDataUtil.getKnownWords(pdfId)
        Log.d(TAG, "getKnownWords: ${words.size}개 단어, PDF ID: $pdfId")
        return words
    }

    fun saveKnownWords(pdfId: Long, words: List<KanjiDetailModel>) {
        val pdfDataUtil = getPdfDataUtil()
        pdfDataUtil.saveKnownWords(pdfId, words)
        Log.d(TAG, "saveKnownWords: ${words.size}개 단어 저장, PDF ID: $pdfId")
    }


    private fun generateKnownChapters(totalWords: Int, pdfId: Long) {
        val count = (totalWords + 9) / 10
        val chapters = List(count) { i ->
            "K:${i + 1}" // 새로운 K: 형식 사용
        }

        val pdfDataUtil = getPdfDataUtil()
        pdfDataUtil.saveKnownChapters(pdfId, chapters)
        Log.d(TAG, "아는 단어 챕터 생성 완료: ${chapters.size}개, PDF ID: $pdfId")
    }

    fun addKnownWords(pdfId: Long, words: List<KanjiDetailModel>) = viewModelScope.launch {
        try {
            Log.d(TAG, "addKnownWords 시작: ${words.size}개 단어, PDF ID: $pdfId")

            updateWordsState(pdfId, words)
            persistWords(pdfId)

            baseEvent(UiEvent.Toast.Normal("${words.size}개 단어가 아는 단어장에 추가되었습니다"))
            delay(300)

            _refreshKnownChapters.emit(words.size.toLong())
            Log.d(TAG, "refreshKnownChapters 이벤트 발생: $pdfId (추가된 단어: ${words.size})")
        } catch (e: Exception) {
            Log.e(TAG, "단어 추가 중 오류: ${e.message}", e)
            baseEvent(UiEvent.Toast.Normal("단어 추가 중 오류가 발생했습니다"))
        }
    }

    fun removeKnownWords(pdfId: Long, words: List<KanjiDetailModel>) = viewModelScope.launch {
        try {
            val current = getKnownWords(pdfId)
            val isRemovingAll = isRemovingAllWords(current, words)

            val updated = if (isRemovingAll) {
                emptyList()
            } else {
                filterRemovedWords(current, words)
            }

            processWordRemoval(pdfId, words, updated)
            baseEvent(UiEvent.Toast.Normal("${words.size}개 단어가 원래 단어장으로 이동되었습니다"))
        } catch (e: Exception) {
            Log.e(TAG, "단어 제거 중 오류: ${e.message}", e)
            baseEvent(UiEvent.Toast.Normal("단어 제거 중 오류가 발생했습니다"))
        }
    }

    private suspend fun updateWordsState(pdfId: Long, newWords: List<KanjiDetailModel>) {
        val currentWords = getKnownWords(pdfId)
        val updatedWords =
            (currentWords + newWords).distinctBy { "${it.vocabularyBookOrder}:${it.kanji}" }
        Log.d(TAG, "업데이트된 단어 목록 (${updatedWords.size}개)")

        saveKnownWords(pdfId, updatedWords)
        updateHiddenStatus(pdfId, newWords, true)

        val pdfDataUtil = getPdfDataUtil()
        pdfDataUtil.setHiddenStatusInitialized(pdfId, true)

        _wordStatusChanged.emit(pdfId to newWords)
        _refreshNormalChapters.emit(pdfId)

        generateKnownChapters(updatedWords.size, pdfId)
    }

    private suspend fun processWordRemoval(
        pdfId: Long,
        words: List<KanjiDetailModel>,
        updated: List<KanjiDetailModel>
    ) {
        saveKnownWords(pdfId, updated)
        updateHiddenStatus(pdfId, words, false)
        _wordStatusChanged.emit(pdfId to words)
        _refreshNormalChapters.emit(pdfId)

        updatePersistentStorage(pdfId, updated)
    }

    private fun isRemovingAllWords(
        current: List<KanjiDetailModel>,
        removing: List<KanjiDetailModel>
    ): Boolean {
        return removing.size == current.size ||
                removing.map { "${it.vocabularyBookOrder}:${it.kanji}" }.toSet() ==
                current.map { "${it.vocabularyBookOrder}:${it.kanji}" }.toSet() ||
                current.size <= 1
    }

    private fun filterRemovedWords(
        current: List<KanjiDetailModel>,
        removing: List<KanjiDetailModel>
    ): List<KanjiDetailModel> {
        return current.filterNot { cur ->
            removing.any { it.vocabularyBookOrder == cur.vocabularyBookOrder && it.kanji == cur.kanji }
        }
    }

    private suspend fun persistWords(pdfId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val words = getKnownWords(pdfId)
                val bookName = _pdfInfo.value?.bookName ?: "PDF $pdfId"
                val result = saveKnownWordKanjiUseCase(bookName, words)
                Log.d(TAG, "저장소에 저장 완료. ID: $result")

                val pdfDataUtil = getPdfDataUtil()
                pdfDataUtil.setKnownWordsLoaded(pdfId, true)
            } catch (e: Exception) {
                Log.e(TAG, "저장소에 저장 실패: ${e.message}", e)
                baseEvent(UiEvent.Toast.Normal("영구 저장에 실패했습니다. 앱을 재시작하면 데이터가 손실될 수 있습니다."))
                throw e
            }
        }
    }

    private suspend fun updatePersistentStorage(pdfId: Long, updatedWords: List<KanjiDetailModel>) {
        try {
            val bookName = _pdfInfo.value?.bookName ?: "PDF $pdfId"

            withContext(Dispatchers.IO) {
                if (updatedWords.isEmpty()) {
                    deleteKnownWordKanjiByBookNameUseCase.deleteUnknownKanjiByBookName(bookName)
                    Log.d(TAG, "모든 단어가 제거되어 저장소에서 삭제: 책 이름 $bookName")
                } else {
                    saveKnownWordKanjiUseCase(bookName, updatedWords)
                    Log.d(TAG, "남은 ${updatedWords.size}개 단어를 저장소에 저장")
                }
            }

            withContext(Dispatchers.Main) {
                val pdfDataUtil = getPdfDataUtil()
                if (updatedWords.isEmpty()) {
                    pdfDataUtil.setAutoLoadDisabled(pdfId, true)
                    pdfDataUtil.saveKnownChapters(pdfId, emptyList())

                    _refreshKnownChapters.emit(0)
                    pdfDataUtil.setKnownWordsLoaded(pdfId, false)
                    pdfDataUtil.setHiddenStatusInitialized(pdfId, false)
                } else {
                    generateKnownChapters(updatedWords.size, pdfId)
                    _refreshKnownChapters.emit(0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "저장소 업데이트 중 오류: ${e.message}", e)
            throw e
        }
    }

    override fun onCleared() {
        super.onCleared()
        pdfInfoCache.clear()
    }
}