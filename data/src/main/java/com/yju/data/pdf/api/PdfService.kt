package com.yju.data.pdf.api

import com.yju.data.pdf.dto.response.PdfFileUploadResponse
import com.yju.data.pdf.dto.response.StatusResponse
import com.yju.domain.util.NetworkState
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PdfService {
    @Multipart
    @POST("api/files/upload")
    suspend fun uploadVocabularyPdf(
        @Part filePart: MultipartBody.Part
    ): NetworkState<Map<String, String>>

    @GET("api/files/{id}/status")
    suspend fun getUploadProcessingStatus (
        @Path("id") id: String
    ): NetworkState<StatusResponse>

    @GET("api/files/{id}/result")
    suspend fun getPdfProcessedResult(
        @Path("id") id: String
    ): NetworkState<PdfFileUploadResponse>

    @Multipart
    @POST("api/files/test-upload")
    suspend fun uploadPdfFile(
        @Part filePart: MultipartBody.Part
    ): NetworkState<PdfFileUploadResponse>
}