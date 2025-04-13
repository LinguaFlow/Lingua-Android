package com.yju.data.pdf.api

import com.yju.data.pdf.dto.response.PdfFileUploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PdfService {
    @Multipart
    @POST("api/files/upload")
    suspend fun uploadPdfFile(
        @Part filePart: MultipartBody.Part
    ): Response<PdfFileUploadResponse>
}