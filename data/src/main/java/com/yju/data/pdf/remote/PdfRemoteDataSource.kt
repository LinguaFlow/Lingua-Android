package com.yju.data.pdf.remote

import com.yju.data.pdf.dto.response.FileUploadResponse
import com.yju.data.pdf.dto.response.PdfFileUploadResponse
import com.yju.domain.util.NetworkState
import okhttp3.MultipartBody

interface PdfRemoteDataSource {
    suspend fun uploadVocabularyPdf(filePart: MultipartBody.Part): NetworkState<FileUploadResponse>
    suspend fun getPdfProcessingStatus(id: Long): NetworkState<FileUploadResponse>
    suspend fun getPdfProcessedResult(id: Long): NetworkState<PdfFileUploadResponse>
    suspend fun deleteVocabulary(vocabularyUploadId: Long): Result<Unit>
    suspend fun cancelUpload(id: Long): NetworkState<Unit>
}
