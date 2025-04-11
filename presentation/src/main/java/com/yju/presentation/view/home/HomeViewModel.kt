package com.yju.presentation.view.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yju.domain.pdf.model.PdfUploadModel
import com.yju.domain.pdf.usecase.PdfJoinUseCase
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.MutableEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pdfJoinUseCase: PdfJoinUseCase
) : BaseViewModel() {

    // 파일 정보
    private val _selectedFileUri = MutableLiveData<Uri?>()
    val selectedFileUri: LiveData<Uri?> = _selectedFileUri

    private val _selectedFileName = MutableLiveData<String>()
    val selectedFileName: LiveData<String> = _selectedFileName

    // 업로드 결과
    private val _uploadResponse = MutableLiveData<PdfUploadModel?>()
    val uploadResponse: LiveData<PdfUploadModel?> = _uploadResponse

    private val _onClickUpload = MutableEventFlow<Boolean>()
    val onClickUpload : EventFlow<Boolean> get() = _onClickUpload


    // 파일 URI, 파일 이름 설정
    fun setSelectedFileUri(uri: Uri?, fileName: String) {
        _selectedFileUri.value = uri
        _selectedFileName.value = fileName
    }

    // UI 초기화
    fun resetUiState() {
        _selectedFileUri.value = null
        _selectedFileName.value = ""
        _uploadResponse.value = null
    }

    /**
     * PDF 파일을 서버로 업로드하는 로직
     */
    fun uploadFile(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            // 로딩 시작
            baseEvent(UiEvent.Loading.Show)

            try {
                // 1) Uri -> 임시 File 로 복사
                val file = withContext(Dispatchers.IO) {
                    uri.toFile(context, fileName)
                }

                // 파일 변환 성공 시 토스트 표시
                baseEvent(UiEvent.Toast.Normal("파일 변환 중..."))

                // 2) Multipart Body 생성
                val requestBody = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

                // 업로드 시작 토스트 표시
                baseEvent(UiEvent.Toast.Normal("파일 업로드 중..."))

                // 3) 서버 업로드
                val result = pdfJoinUseCase(filePart)  // invoke 연산자를 통한 호출

                result.fold(
                    onSuccess = { response ->
                        _uploadResponse.value = response
                        // 업로드 성공 토스트 표시
                        baseEvent(UiEvent.Toast.Success("파일 '$fileName' 업로드가 완료되었습니다."))
                    },
                    onFailure = { exception ->
                        // 업로드 실패 토스트 표시
                        val errorMsg = exception.message ?: "알 수 없는 오류가 발생했습니다."
                        baseEvent(UiEvent.Toast.Normal("업로드 실패: $errorMsg"))
                    }
                )

                // 임시 파일 삭제
                withContext(Dispatchers.IO) {
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                // 예외 상황 (네트워크 오류 등) 토스트 표시
                val errorMsg = e.message ?: "알 수 없는 오류가 발생했습니다."
                baseEvent(UiEvent.Toast.Normal("오류 발생: $errorMsg"))
            } finally {
                // 로딩 종료
                baseEvent(UiEvent.Loading.Hide)
            }
        }
    }

    /**
     * Uri 확장 함수로 File 변환 기능 제공
     */
    private fun Uri.toFile(context: Context, fileName: String): File {
        val tempFile = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(this)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}