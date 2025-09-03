package com.yju.presentation.view.pdf.upload

import android.net.Uri
import android.util.Log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yju.domain.kanji.model.KanjiModel
import com.yju.domain.kanji.usecase.DeleteKanjiVocabularyCase
import com.yju.domain.kanji.usecase.GetAllKanjiVocabularyUseCase
import com.yju.domain.kanji.usecase.SaveKanjiVocabularyUseCase
import com.yju.domain.known.usecase.DeleteKnownWordKanjiByBookNameUseCase
import com.yju.domain.pdf.usecase.AsyncUploadVocabularyPdfUseCase
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.ext.safeCall
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.MutableEventFlow
import com.yju.presentation.util.asEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    private val getAllKanji: GetAllKanjiVocabularyUseCase,
    private val deleteKanji: DeleteKanjiVocabularyCase,
    private val deleteKnownWordKanjiByBookNameUseCase: DeleteKnownWordKanjiByBookNameUseCase,
    private val asyncUploadPdfUseCase: AsyncUploadVocabularyPdfUseCase,
    private val installKanjiUseCase: SaveKanjiVocabularyUseCase
) : BaseViewModel() {

    companion object {
        private const val TAG = "PdfViewerViewModel"
    }

    private val _onClickNavigateToChapter = MutableEventFlow<Long>()
    val onClickNavigateToChapter: EventFlow<Long> = _onClickNavigateToChapter.asEventFlow()

    private val _onDeleteCompleted = MutableEventFlow<Boolean>()
    val onDeleteCompleted: EventFlow<Boolean> = _onDeleteCompleted.asEventFlow()

    private val _pdfList = MutableLiveData<List<KanjiModel>>(emptyList())
    val pdfList: LiveData<List<KanjiModel>> = _pdfList

    private val _selectedFileUri = MutableLiveData<Uri?>(null)
    val selectedFileUri: LiveData<Uri?> = _selectedFileUri

    private val _selectedFileName = MutableLiveData<String?>(null)
    val selectedFileName: LiveData<String?> = _selectedFileName

    private val _showMenu = MutableEventFlow<Unit>()
    val showMenu: EventFlow<Unit> = _showMenu

    private var isDataLoaded = false

    fun getAsyncUploadUseCase(): AsyncUploadVocabularyPdfUseCase = asyncUploadPdfUseCase


    fun getInstallKanjiUseCase(): SaveKanjiVocabularyUseCase = installKanjiUseCase

    fun setSelectedFile(uri: Uri?, name: String?) {
        _selectedFileUri.value = uri
        _selectedFileName.value = name
        Log.d(TAG, "파일 선택됨: $name")
    }

    fun onClickMenu() = viewModelScope.launch {
        _showMenu.emit(Unit)
    }


    fun navigateToPdfChapter(pdfId: Long) = viewModelScope.launch {
        Log.d(TAG, "PDF 챕터로 네비게이션: $pdfId")
        _onClickNavigateToChapter.emit(pdfId)
    }

    fun refreshList(forceRefresh: Boolean = false) {
        if (isDataLoaded && !forceRefresh) {
            Log.d(TAG, "이미 로드됨, 새로고침 스킵")
            return
        }

        Log.d(TAG, "PDF 목록 새로고침 시작")
        loadPdfList()
        isDataLoaded = true
    }

    private fun loadPdfList() = viewModelScope.launch {
        Log.d(TAG, "PDF 목록 로드 시작")

        safeCall {
            withContext(Dispatchers.IO) {
                getAllKanji()
            }
        }
            .onSuccess { pdfList ->
                Log.d(TAG, "PDF 목록 로드 성공: ${pdfList.size}개")
                _pdfList.postValue(pdfList)
            }
            .onFailure { error ->
                Log.e(TAG, "PDF 목록 로드 실패", error)
                baseEvent(UiEvent.Toast.Normal("목록 불러오기 실패: ${error.message}"))
            }
    }

    fun deletePdf(id: Long) = viewModelScope.launch {
        try {
            Log.d(TAG, "PDF 삭제 시작: ID=$id")

            // 1. 현재 목록에서 해당 PDF 정보 찾기
            val pdfToDelete = _pdfList.value?.find { it.id == id }
            if (pdfToDelete == null) {
                Log.w(TAG, "삭제할 PDF를 찾을 수 없음: ID=$id")
                baseEvent(UiEvent.Toast.Normal("삭제할 PDF를 찾을 수 없습니다"))
                return@launch
            }

            val bookName = pdfToDelete.bookName
            Log.d(TAG, "삭제할 PDF 정보: ID=$id, 책 이름=$bookName")

            // 2. PDF 삭제 및 관련 데이터 삭제
            withContext(Dispatchers.IO) {
                try {
                    // 관련 아는 단어 삭제
                    deleteKnownWordKanjiByBookNameUseCase.deleteUnknownKanjiByBookName(bookName)
                    Log.d(TAG, "관련 아는 단어 삭제 완료: 책 이름=$bookName")
                } catch (e: Exception) {
                    Log.e(TAG, "아는 단어 삭제 실패: ${e.message}", e)
                    // 아는 단어 삭제 실패해도 PDF 삭제는 계속 진행
                }

                // PDF 삭제
                deleteKanji(id)
                Log.d(TAG, "PDF 삭제 완료: ID=$id")
            }

            // 3. 성공 메시지 표시
            baseEvent(UiEvent.Toast.Success("PDF 및 관련 데이터 삭제 완료"))
            val currentList = _pdfList.value ?: emptyList()
            val updatedList = currentList.filter { it.id != id }
            _pdfList.postValue(updatedList)
            Log.d(TAG, "삭제 후 남은 PDF 개수: ${updatedList.size}")
            // 5. 목록이 비어있으면 삭제 완료 이벤트 발행
            if (updatedList.isEmpty()) {
                Log.d(TAG, "모든 PDF 삭제됨, 삭제 완료 이벤트 발행")
                _onDeleteCompleted.emit(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "PDF 삭제 중 오류 발생: ${e.message}", e)
            baseEvent(UiEvent.Toast.Normal("삭제 실패: ${e.message}"))
        }
    }

    fun startUpload() {
        baseEvent(UiEvent.Loading.Show)
    }

    fun handleUploadSuccess() {
        refreshList(forceRefresh = true)
        baseEvent(UiEvent.Toast.Success("PDF 업로드 완료"))
        baseEvent(UiEvent.Loading.Hide)
    }

    fun handleUploadError(message: String) {
        Log.e(TAG, "업로드 오류: $message")
        baseEvent(UiEvent.Toast.Normal(message))
        baseEvent(UiEvent.Loading.Hide)
    }

    override fun onCleared() {
        super.onCleared()
    }
}