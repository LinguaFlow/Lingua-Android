package com.yju.domain.pdf.usecase

import com.yju.domain.pdf.model.UploadTaskStatus
import com.yju.domain.pdf.repository.PdfRepository
import com.yju.domain.util.AsyncUploadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import javax.inject.Inject


class AsyncUploadVocabularyPdfUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) {

    operator fun invoke(
        file: MultipartBody.Part,
        pollingInterval: Long = 30_000  // 30초로 변경
    ): Flow<AsyncUploadState> = flow {
        emit(AsyncUploadState.Uploading)

        val taskId = pdfRepository.uploadVocabularyPdf(file)
            .getOrElse { error ->
                emit(AsyncUploadState.Error(error))
                return@flow
            }

        emit(AsyncUploadState.Submitted(taskId))

        var currentStatus: UploadTaskStatus

        while (true) {
            delay(pollingInterval)  // 30초마다 상태 확인

            currentStatus = pdfRepository.getPdfProcessingStatus(taskId)
                .getOrElse { error ->
                    emit(AsyncUploadState.Error(error))
                    return@flow
                }

            when (currentStatus) {
                UploadTaskStatus.DONE -> break                         // 루프 탈출
                UploadTaskStatus.FAILED -> {
                    emit(AsyncUploadState.Error(Exception("Processing failed")))
                    return@flow                                        // 플로우 종료
                }
                else -> emit(AsyncUploadState.Processing(currentStatus))
            }
        }

        // 여기까지 왔다는 것은 currentStatus == DONE
        val result = pdfRepository.getPdfProcessedResult(taskId)
            .getOrElse { error ->
                emit(AsyncUploadState.Error(error))
                return@flow
            }

        emit(AsyncUploadState.Success(result))
    }
}