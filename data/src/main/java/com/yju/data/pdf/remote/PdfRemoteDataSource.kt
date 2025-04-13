package com.yju.data.pdf.remote

import com.yju.data.pdf.dto.response.PdfFileUploadResponse
import okhttp3.MultipartBody

interface PdfRemoteDataSource {
    suspend fun uploadPdfFile(filePart: MultipartBody.Part): Result<PdfFileUploadResponse>
}