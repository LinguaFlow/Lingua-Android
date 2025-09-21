package com.yju.domain.pdf.usecase

import com.yju.domain.pdf.model.UploadTaskStatus
import com.yju.domain.pdf.repository.PdfRepository
import com.yju.domain.util.AsyncUploadState
import com.yju.domain.util.WebSocketManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import okhttp3.MultipartBody
import javax.inject.Inject


class AsyncUploadVocabularyPdfUseCase @Inject constructor(
    private val pdfRepository: PdfRepository,
    private val webSocketManager: WebSocketManager
) {
    private var currentTaskId: Long? = null

    operator fun invoke(
        file: MultipartBody.Part,
        pollingInterval: Long = 5_000
    ): Flow<AsyncUploadState> = flow {
        emit(AsyncUploadState.Uploading)

        val taskId = pdfRepository.uploadVocabularyPdf(file)
            .getOrElse { error ->
                emit(AsyncUploadState.Error(error))
                return@flow
            }

        currentTaskId = taskId
        emit(AsyncUploadState.Submitted(taskId))

        // WebSocket 연결 시도
        val useWebSocket = try {
            webSocketManager.connect().isSuccess
        } catch (e: Exception) {
            false
        }

        if (useWebSocket) {
            // WebSocket 구독
            try {
                webSocketManager.subscribeToUploadStatus(taskId)
                    .collect { message ->
                        when (UploadTaskStatus.from(message.status)) {
                            UploadTaskStatus.DONE -> {
                                val result = pdfRepository.getPdfProcessedResult(taskId)
                                    .getOrThrow()
                                emit(AsyncUploadState.Success(result))
                            }

                            UploadTaskStatus.FAILED -> {
                                emit(AsyncUploadState.Error(Exception("Processing failed")))
                            }

                            else -> {
                                emit(AsyncUploadState.Processing(UploadTaskStatus.from(message.status)))
                            }
                        }
                    }
            } catch (e: Exception) {
                handlePolling(taskId, pollingInterval)
            }
        } else {
            handlePolling(taskId, pollingInterval)
        }
    }.onCompletion {
        currentTaskId = null
        webSocketManager.disconnect()
    }

    private suspend fun FlowCollector<AsyncUploadState>.handlePolling(
        taskId: Long,
        interval: Long
    ) {
        while (currentTaskId != null) {
            delay(interval)

            val status = pdfRepository.getPdfProcessingStatus(taskId)
                .getOrElse { error ->
                    emit(AsyncUploadState.Error(error))
                    return
                }

            when (status) {
                UploadTaskStatus.DONE -> {
                    val result = pdfRepository.getPdfProcessedResult(taskId)
                        .getOrThrow()
                    emit(AsyncUploadState.Success(result))
                    break
                }

                UploadTaskStatus.FAILED -> {
                    emit(AsyncUploadState.Error(Exception("Processing failed")))
                    break
                }

                else -> emit(AsyncUploadState.Processing(status))
            }
        }
    }
}