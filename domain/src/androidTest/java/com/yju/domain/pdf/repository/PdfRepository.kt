package com.yju.domain.pdf.repository


import com.yju.domain.pdf.model.PdfModel
import com.yju.domain.pdf.model.UploadTaskStatus
import com.yju.domain.util.NetworkState
import okhttp3.MultipartBody

interface PdfRepository {
    suspend fun uploadPdfFileForTest(filePart: MultipartBody.Part): Result<PdfModel>

    // 업로드
    suspend fun uploadVocabularyPdf(filePart: MultipartBody.Part): Result<String>

    // 상태 확인
    suspend fun getPdfProcessingStatus(id: String): Result<UploadTaskStatus>

    // 결과 조회
    suspend fun getPdfProcessedResult(id: String): Result<PdfModel>
}