package com.yju.presentation.view.pdf.upload

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.yju.domain.kanji.usecase.SaveKanjiVocabularyUseCase
import com.yju.domain.pdf.model.PdfModel
import com.yju.domain.pdf.model.UploadTaskStatus
import com.yju.domain.pdf.usecase.AsyncUploadVocabularyPdfUseCase
import com.yju.domain.pdf.usecase.CancelUploadUseCase
import com.yju.domain.util.AsyncUploadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean


class UploadPdfDelegate(
    private val owner: Fragment,
    private val cancelUploadUseCase: CancelUploadUseCase,
    private val asyncUpload: AsyncUploadVocabularyPdfUseCase,
    private val installKanji: SaveKanjiVocabularyUseCase,
    private val coroutineScope: CoroutineScope,
    private val onFileSelected: (Uri, String) -> Unit,
    private val onUploadStarted: () -> Unit,
    private val onProcessing: (UploadTaskStatus) -> Unit = {},
    private val onUploadSuccess: (PdfModel) -> Unit,
    private val onUploadFailed: (String) -> Unit,
    private val pickPdfLauncher: ActivityResultLauncher<Array<String>>,
    private val permLauncher: ActivityResultLauncher<String>
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "UploadPdfDelegate"
        const val PDF_MIME_TYPE = "application/pdf"
        const val DEFAULT_FILENAME = "unknown.pdf"
        private val PERMISSION_REQUIRED_VERSIONS = 23..28
    }
    private val context get() = owner.requireContext()
    private var currentTaskId: Long? = null
    private var currentUploadUri: Uri? = null
    private var currentUploadName: String = ""
    private var uploadJob: Job? = null
    private val isCancelled = AtomicBoolean(false)

    override fun onDestroy(owner: LifecycleOwner) {
        try {
            cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy cleanup error: ${e.message}")
        }
        super.onDestroy(owner)
    }

    /**
     * PDF 파일 선택기 실행
     */
    fun launchPicker() {
        Log.d(TAG, "PDF 파일 선택기 실행")

        val needPermission = Build.VERSION.SDK_INT in PERMISSION_REQUIRED_VERSIONS &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED

        if (needPermission) {
            Log.d(TAG, "저장소 권한 요청 필요")
            permLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            Log.d(TAG, "PDF 파일 선택기 직접 실행")
            pickPdfLauncher.launch(arrayOf(PDF_MIME_TYPE))
        }
    }

    /**
     * 선택된 파일 업로드 실행
     */
    fun uploadFile(uri: Uri, name: String) {
        Log.d(TAG, "파일 업로드 시작: $name")

        // 이전 업로드 작업 취소
        cancelCurrentUpload()

        // 취소 플래그 초기화
        isCancelled.set(false)

        // 업로드 시작 알림
        onUploadStarted()

        // 권한 확인 후 업로드
        checkPermissionAndUpload(uri, name)
    }

    fun cancelUpload() {
        Log.d(TAG, "업로드 취소 요청 - Task ID: $currentTaskId")

        val taskIdToCancel = currentTaskId
        isCancelled.set(true)
        cancelCurrentUpload()

        // 서버에 취소 요청
        taskIdToCancel?.let { taskId ->
            owner.lifecycleScope.launch {
                cancelUploadUseCase.invoke(taskId)
                    .onSuccess {
                        Log.d(TAG, "서버 취소 성공")
                    }
                    .onFailure { error ->
                        Log.e(TAG, "서버 취소 실패: ${error.message}")
                    }
            }
        }

        // 상태 초기화 및 콜백 호출
        resetUploadState()
        onUploadFailed("업로드가 취소되었습니다")
    }

    fun handleSelectedFile(uri: Uri) {
        Log.d(TAG, "선택된 파일 처리: $uri")

        runCatching {
            // URI 권한을 영구적으로 유지
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            // 파일 이름 추출 및 콜백 호출
            val fileName = uri.getFileName(context.contentResolver)
            Log.d(TAG, "파일명 추출 완료: $fileName")
            onFileSelected(uri, fileName)

        }.onFailure { exception ->
            Log.e(TAG, "파일 선택 처리 실패", exception)
            onUploadFailed("파일 선택 오류: ${exception.message}")
        }
    }

    fun performUpload() {
        Log.d(TAG, "권한 허용 후 업로드 진행")

        currentUploadUri?.let { uri ->
            if (currentUploadName.isNotEmpty()) {
                performUploadInternal(uri, currentUploadName)
            } else {
                Log.w(TAG, "업로드할 파일 이름이 없습니다")
                onUploadFailed("업로드할 파일 정보가 없습니다")
            }
        } ?: run {
            Log.w(TAG, "업로드할 파일 URI가 없습니다")
            onUploadFailed("업로드할 파일 정보가 없습니다")
        }
    }

    fun cleanup() {
        cancelCurrentUpload()
        resetUploadState()
    }

    private fun checkPermissionAndUpload(uri: Uri, name: String) {
        val needPermission = Build.VERSION.SDK_INT in PERMISSION_REQUIRED_VERSIONS &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED

        if (needPermission) {
            Log.d(TAG, "권한 필요 - 업로드 정보 저장 후 권한 요청")
            // 업로드 정보 저장
            currentUploadUri = uri
            currentUploadName = name
            // 권한 요청
            permLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            Log.d(TAG, "권한 확인 완료 - 업로드 진행")
            performUploadInternal(uri, name)
        }
    }

    /**
     * 실제 업로드 처리 (취소 확인 로직 추가)
     */
    private fun performUploadInternal(uri: Uri, name: String) {
        Log.d(TAG, "실제 업로드 처리 시작: $name")

        uploadJob = coroutineScope.launch {
            try {
                if (isCancelled.get()) {
                    Log.d(TAG, "업로드 시작 전 취소됨")
                    return@launch
                }

                val part = MultipartBody.Part.createFormData(
                    "file", name, uri.asPdfRequestBody(context.contentResolver)
                )

                asyncUpload(part).collectLatest { state ->
                    if (isCancelled.get()) {
                        Log.d(TAG, "업로드 중 취소됨")
                        return@collectLatest
                    }

                    handleUploadState(state)
                }

            } catch (exception: Exception) {
                if (isCancelled.get()) {
                    Log.d(TAG, "업로드 취소로 인한 예외: ${exception.message}")
                } else {
                    Log.e(TAG, "업로드 처리 중 오류 발생", exception)
                    onUploadFailed("업로드 오류: ${exception.message}")
                }
                resetUploadState()
            }
        }
    }

    /**
     * 업로드 상태 처리 (취소 확인 로직 추가)
     */
    private suspend fun handleUploadState(state: AsyncUploadState) {
        // 취소 확인
        if (isCancelled.get()) {
            Log.d(TAG, "상태 처리 중 취소됨")
            return
        }

        Log.d(TAG, "업로드 상태: ${state::class.simpleName}")

        when (state) {
            is AsyncUploadState.Uploading -> {
                Log.d(TAG, "업로드 진행 중...")
                onUploadStarted()
            }

            is AsyncUploadState.Processing -> {
                Log.d(TAG, "서버에서 처리 중: ${state.status}")
                onProcessing(state.status)
            }

            is AsyncUploadState.Submitted -> {
                Log.d(TAG, "업로드 제출 완료 - Task ID: ${state.taskId}")
                currentTaskId = state.taskId  // Task ID 저장
                onProcessing(UploadTaskStatus.PENDING)
            }

            is AsyncUploadState.Success -> {
                handleUploadSuccess(state.result)
                uploadJob?.cancel()
                uploadJob = null
            }

            is AsyncUploadState.Error -> {
                Log.e(TAG, "업로드 실패: ${state.error.message}")
                handleUploadError(state.error)
            }
        }
    }

    private suspend fun handleUploadSuccess(result: PdfModel) {
        if (isCancelled.get()) {
            return
        }
        try {
            Timber.tag(TAG).d("한자 설치 시작 - bookName: ${result.bookName}, word count: ${result.word.size}")
            installKanji(result.bookName, result.word)
            Timber.tag(TAG).d("한자 설치 완료")
            onUploadSuccess(result)
        } catch (exception: Exception) {
            Timber.tag(TAG).e(exception, "한자 설치 실패: ${exception.message}")
            onUploadFailed("한자 설치 오류: ${exception.message}")
        } finally {
            resetUploadState()
        }
    }

    private fun handleUploadError(error: Throwable) {
        if (isCancelled.get()) {
            Timber.tag(TAG).d("취소로 인한 오류 무시")
            return
        }
        val errorMessage = error.message ?: "알 수 없는 오류"
        onUploadFailed(errorMessage)
        resetUploadState()
    }

    private fun cancelCurrentUpload() {
        uploadJob?.cancel()
        uploadJob = null
    }

    private fun resetUploadState() {
        currentUploadUri = null
        currentUploadName = ""
        currentTaskId = null  // 추가
        isCancelled.set(false)
    }
}

fun Uri.getFileName(contentResolver: ContentResolver): String {
    return try {
        contentResolver.query(
            this,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else null
            } else null
        } ?: lastPathSegment ?: UploadPdfDelegate.DEFAULT_FILENAME

    } catch (exception: Exception) {
        Log.w("UriExtensions", "파일 이름 추출 실패", exception)
        lastPathSegment ?: UploadPdfDelegate.DEFAULT_FILENAME
    }
}


private fun Uri.asPdfRequestBody(contentResolver: ContentResolver): RequestBody {
    return object : RequestBody() {

        override fun contentType() = UploadPdfDelegate.PDF_MIME_TYPE.toMediaType()

        override fun contentLength(): Long {
            return try {
                contentResolver.openAssetFileDescriptor(this@asPdfRequestBody, "r")
                    ?.use { it.length } ?: -1L
            } catch (exception: Exception) {
                Log.w("UriExtensions", "파일 크기 확인 실패", exception)
                -1L
            }
        }

        override fun writeTo(sink: BufferedSink) {
            try {
                contentResolver.openInputStream(this@asPdfRequestBody)?.use { inputStream ->
                    sink.writeAll(inputStream.source())
                } ?: throw IllegalStateException("InputStream을 열 수 없습니다")

            } catch (exception: Exception) {
                Log.e("UriExtensions", "파일 쓰기 실패", exception)
                throw exception
            }
        }
    }
}