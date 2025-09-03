package com.yju.data.pdf.api

import com.yju.data.pdf.dto.response.FileUploadResponse
import com.yju.data.pdf.dto.response.PdfFileUploadResponse
import com.yju.domain.util.NetworkState
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PdfService {

    @Multipart
    @POST("api/files/upload")
    @Headers("Auth: true")
    suspend fun uploadVocabularyPdf(
        @Part filePart: MultipartBody.Part
    ): NetworkState<FileUploadResponse>  // Map<String, String>에서 FileUploadResponse로 변경

    @Headers("Auth: true")
    @GET("api/files/{id}/status")
    suspend fun getUploadProcessingStatus(
        @Path("id") id: Long  // String에서 Long으로 변경
    ): NetworkState<FileUploadResponse>

    @Headers("Auth: true")
    @GET("api/files/{id}/result")
    suspend fun getPdfProcessedResult(
        @Path("id") id: Long  // String에서 Long으로 변경
    ): NetworkState<PdfFileUploadResponse>
}