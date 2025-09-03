package com.yju.presentation.view.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yju.domain.pdf.model.PdfModel
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.MutableEventFlow
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.Navigation
import com.yju.presentation.util.asEventFlow
import com.yju.presentation.view.pdf.chapter.PdfChapterFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import javax.inject.Inject

/**
 * HomeViewModel - 홈 화면 상태 관리 및 네비게이션 제어
 * 성능 최적화 리팩토링
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : BaseViewModel() {
    companion object {
        private const val TAG = "HomeViewModel"
        private const val MIN_NAVIGATION_INTERVAL = 500L  // 네비게이션 최소 간격(ms)
    }

    // 백그라운드 작업을 위한 IO 스코프
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 작업 취소를 위한 Job 맵
    private val jobMap = mutableMapOf<String, Job>()

    private var lastNavigationTime = 0L

    private val _navigationEvent = MutableEventFlow<Navigation>()
    val navigationEvent: EventFlow<Navigation> = _navigationEvent.asEventFlow()

    // ─── 파일 선택 및 업로드 상태 ────────────────────────────────────
    private val _selectedFileUri = MutableLiveData<Uri?>()
    val selectedFileUri: LiveData<Uri?> = _selectedFileUri

    private val _selectedFileName = MutableLiveData<String?>()
    val selectedFileName: LiveData<String?> = _selectedFileName

    private val _isUploading = MutableLiveData(false)
    val isUploading: LiveData<Boolean> = _isUploading

    /**
     * ViewModel 정리
     */
    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
        jobMap.values.forEach { it.cancel() }
        jobMap.clear()
    }

    protected fun showLoading() {
        baseEvent(UiEvent.Loading.Show)
    }

    protected fun hideLoading() {
        baseEvent(UiEvent.Loading.Hide)
    }

    private fun emitNavigation(event: Navigation) {
        val currentTime = System.currentTimeMillis()

        // 중복 네비게이션 방지 - 최소 간격 체크
        if (currentTime - lastNavigationTime < MIN_NAVIGATION_INTERVAL) {
            Log.d(TAG, "네비게이션 무시: 최소 간격 내 ($MIN_NAVIGATION_INTERVAL ms)")
            return
        }

        lastNavigationTime = currentTime

        viewModelScope.launch {
            try {
                Log.d(TAG, "네비게이션 이벤트 발행: $event")
                _navigationEvent.emit(event)
            } catch (e: Exception) {
                Log.e(TAG, "네비게이션 이벤트 발행 실패: ${e.message}", e)
            }
        }
    }

    fun navigateToPdfUpload() {
        navigateToPdfViewer()
    }

    fun navigateToPdfViewer(bookId: String? = null) {
        emitNavigation(Navigation.ToPdfViewer(bookId))
    }

    fun navigateToPdfChapter(pdfId: Long) {
        emitNavigation(Navigation.ToPdfChapter(pdfId))
    }

    fun setSelectedFileUri(uri: Uri?, name: String?) {
        _selectedFileUri.value = uri
        _selectedFileName.value = name
        Log.d(TAG, "파일 선택: ${name ?: "없음"}")
    }

    fun setUploading(uploading: Boolean) {
        _isUploading.value = uploading
        if (uploading) {
            showLoading()
        } else {
            hideLoading()
        }
        Log.d(TAG, "업로드 상태 변경: $uploading (로딩 다이얼로그 ${if(uploading) "표시" else "숨김"})")
    }

}