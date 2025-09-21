package com.yju.domain.pdf.repository


import com.yju.domain.pdf.model.PdfModel
import com.yju.domain.pdf.model.UploadTaskStatus
import okhttp3.MultipartBody

interface PdfRepository {

    suspend fun uploadVocabularyPdf(filePart: MultipartBody.Part): Result<Long>

    suspend fun getPdfProcessingStatus(id: Long): Result<UploadTaskStatus>

    suspend fun getPdfProcessedResult(id: Long): Result<PdfModel>

    suspend fun deleteVocabulary(vocabularyUploadId: Long): Result<Unit>

    suspend fun cancelUpload(id: Long): Result<Unit>
}