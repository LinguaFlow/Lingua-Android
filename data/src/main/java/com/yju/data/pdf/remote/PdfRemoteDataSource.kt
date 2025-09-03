package com.yju.data.pdf.remote

import com.yju.data.pdf.dto.response.FileUploadResponse
import com.yju.data.pdf.dto.response.PdfFileUploadResponse
import com.yju.domain.util.NetworkState
import okhttp3.MultipartBody

interface PdfRemoteDataSource {
    suspend fun uploadVocabularyPdf(filePart: MultipartBody.Part): NetworkState<FileUploadResponse>

    // 처리 상태 확인
    suspend fun getPdfProcessingStatus(id: Long): NetworkState<FileUploadResponse>

    // 처리 결과 조회
    suspend fun getPdfProcessedResult(id: Long): NetworkState<PdfFileUploadResponse>
}
