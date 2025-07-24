package com.yju.data.pdf.remote

import com.yju.data.pdf.api.PdfService
import com.yju.data.pdf.dto.response.PdfFileUploadResponse
import com.yju.data.pdf.dto.response.StatusResponse
import com.yju.domain.util.NetworkState
import okhttp3.MultipartBody
import javax.inject.Inject

class PdfRemoteDataSourceImpl @Inject constructor(
    private val pdfService: PdfService
) : PdfRemoteDataSource {

    override suspend fun uploadVocabularyPdf(filePart: MultipartBody.Part): NetworkState<Map<String, String>> {
        return pdfService.uploadVocabularyPdf(filePart)
    }

    override suspend fun getPdfProcessingStatus(id: String): NetworkState<StatusResponse> {
        return pdfService.getUploadProcessingStatus(id)
    }

    override suspend fun getPdfProcessedResult(id: String): NetworkState<PdfFileUploadResponse> {
        return pdfService.getPdfProcessedResult(id)
    }

    // 테스트 용도로 사용중 추후 삭제 예정
    override suspend fun uploadTestPdf(filePart: MultipartBody.Part): NetworkState<PdfFileUploadResponse> {
        return pdfService.uploadPdfFile(filePart)
    }

}
