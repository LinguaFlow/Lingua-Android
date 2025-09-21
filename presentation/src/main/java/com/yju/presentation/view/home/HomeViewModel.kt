package com.yju.presentation.view.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.MutableEventFlow
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.Navigation
import com.yju.presentation.util.asEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : BaseViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
        private const val MIN_NAVIGATION_INTERVAL = 500L
    }

    private var lastNavigationTime = 0L

    private val _navigationEvent = MutableEventFlow<Navigation>()
    val navigationEvent: EventFlow<Navigation> = _navigationEvent.asEventFlow()

    private val _selectedFileUri = MutableLiveData<Uri?>()
    val selectedFileUri: LiveData<Uri?> = _selectedFileUri

    private val _selectedFileName = MutableLiveData<String?>()
    val selectedFileName: LiveData<String?> = _selectedFileName

    private val _isUploading = MutableLiveData(false)
    val isUploading: LiveData<Boolean> = _isUploading

    private fun emitNavigation(event: Navigation) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastNavigationTime < MIN_NAVIGATION_INTERVAL) {
            Log.d(TAG, "네비게이션 무시: 최소 간격 내")
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
            baseEvent(UiEvent.Loading.Show)
        } else {
            baseEvent(UiEvent.Loading.Hide)
        }
        Log.d(TAG, "업로드 상태 변경: $uploading")
    }
}