package com.yju.domain.pdf.repository

import com.yju.domain.pdf.model.PdfUploadModel
import okhttp3.MultipartBody

interface PdfRepository {
    suspend fun uploadPdfFile(filePart: MultipartBody.Part): Result<PdfUploadModel>
}