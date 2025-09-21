package com.yju.data.pdf.remote

import com.yju.data.pdf.api.PdfService
import com.yju.data.pdf.dto.response.FileUploadResponse
import com.yju.data.pdf.dto.response.PdfFileUploadResponse
import com.yju.domain.auth.util.SharedPreferenceUtil
import com.yju.domain.util.NetworkState
import okhttp3.MultipartBody
import javax.inject.Inject

class PdfRemoteDataSourceImpl @Inject constructor(
    private val pdfService: PdfService,
) : PdfRemoteDataSource {

    override suspend fun uploadVocabularyPdf(filePart: MultipartBody.Part): NetworkState<FileUploadResponse> {
        return pdfService.uploadVocabularyPdf(filePart)
    }

    override suspend fun getPdfProcessingStatus(id: Long): NetworkState<FileUploadResponse> {
        return pdfService.getUploadProcessingStatus(id)
    }

    override suspend fun getPdfProcessedResult(id: Long): NetworkState<PdfFileUploadResponse> {
        return pdfService.getPdfProcessedResult(id)
    }

    override suspend fun deleteVocabulary(vocabularyUploadId: Long): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun cancelUpload(id: Long): NetworkState<Unit> {
        return pdfService.cancelUpload(id)
    }
}
