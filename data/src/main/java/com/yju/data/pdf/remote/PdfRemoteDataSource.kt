package com.yju.data.pdf.remote

import com.yju.data.pdf.dto.response.PdfFileUploadResponse
import com.yju.data.pdf.dto.response.StatusResponse
import com.yju.domain.util.NetworkState
import okhttp3.MultipartBody

interface PdfRemoteDataSource {
    suspend fun uploadVocabularyPdf(filePart: MultipartBody.Part): NetworkState<Map<String, String>>

    // 처리 상태 확인
    suspend fun getPdfProcessingStatus(id: String): NetworkState<StatusResponse>

    // 처리 결과 조회
    suspend fun getPdfProcessedResult(id: String): NetworkState<PdfFileUploadResponse>

    // 테스트용 업로드 (추후 삭제 예정)
    suspend fun uploadTestPdf(filePart: MultipartBody.Part): NetworkState<PdfFileUploadResponse>


}
