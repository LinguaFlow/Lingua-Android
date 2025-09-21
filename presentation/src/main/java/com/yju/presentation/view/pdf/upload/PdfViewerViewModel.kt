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
import com.yju.domain.pdf.usecase.CancelUploadUseCase
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.ext.safeCall
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.MutableEventFlow
import com.yju.presentation.util.asEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    private val getAllKanji: GetAllKanjiVocabularyUseCase,
    private val deleteKanji: DeleteKanjiVocabularyCase,
    private val asyncUploadPdfUseCase: AsyncUploadVocabularyPdfUseCase,
    private val installKanjiUseCase: SaveKanjiVocabularyUseCase,
    private val cancelUploadUseCase: CancelUploadUseCase
) : BaseViewModel() {

    companion object {
        private const val TAG = "PdfViewerViewModel"
    }


    private val _onClickNavigateToChapter = MutableEventFlow<Long>()
    val onClickNavigateToChapter: EventFlow<Long> = _onClickNavigateToChapter.asEventFlow()

    private val _onDeleteCompleted = MutableEventFlow<Boolean>()
    val onDeleteCompleted: EventFlow<Boolean> = _onDeleteCompleted.asEventFlow()

    private var currentUploadJob: Job? = null

    private val _pdfList = MutableLiveData<List<KanjiModel>>(emptyList())
    val pdfList: LiveData<List<KanjiModel>> = _pdfList

    private val _selectedFileUri = MutableLiveData<Uri?>(null)
    val selectedFileUri: LiveData<Uri?> = _selectedFileUri

    private val _selectedFileName = MutableLiveData<String?>(null)
    val selectedFileName: LiveData<String?> = _selectedFileName

    private val _showMenu = MutableEventFlow<Unit>()
    val showMenu: EventFlow<Unit> = _showMenu

    private var isDataLoaded = false

    fun getCancelUploadUseCase(): CancelUploadUseCase = cancelUploadUseCase

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
        safeCall {
            withContext(Dispatchers.IO) {
                getAllKanji()
            }
        }.onSuccess { pdfList ->
            Log.d(TAG, "PDF 목록 로드 성공: ${pdfList.size}개")
            _pdfList.postValue(pdfList)
        }.onFailure { error ->
            Log.e(TAG, "PDF 목록 로드 실패", error)
            baseEvent(UiEvent.Toast.Normal("목록 불러오기 실패: ${error.message}"))
        }
    }

    fun deletePdf(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        val pdfToDelete = _pdfList.value?.find { it.id == id }
        if (pdfToDelete == null) {
            Log.w(TAG, "삭제할 PDF를 찾을 수 없음: ID=$id")
            baseEvent(UiEvent.Toast.Normal("삭제할 PDF를 찾을 수 없습니다"))
            return@launch
        }

        val bookName = pdfToDelete.bookName
        deleteKanji(id)

        val updatedList = _pdfList.value?.filter { it.id != id } ?: emptyList()
        _pdfList.postValue(updatedList)

        baseEvent(UiEvent.Toast.Success("PDF 삭제 완료"))
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

    fun cancelAllUploadTasks() {
        currentUploadJob?.cancel()
        currentUploadJob = null
    }

    override fun onCleared() {
        cancelAllUploadTasks()
        super.onCleared()
    }
}