package com.yju.data.pdf.api

import com.yju.data.pdf.dto.response.FileUploadResponse
import com.yju.data.pdf.dto.response.PdfFileUploadResponse
import com.yju.domain.util.NetworkState
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.GET
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
    ): NetworkState<FileUploadResponse>

    @Headers("Auth: true")
    @GET("api/files/{id}/status")
    suspend fun getUploadProcessingStatus(
        @Path("id") id: Long
    ): NetworkState<FileUploadResponse>

    @Headers("Auth: true")
    @GET("api/files/{id}/result")
    suspend fun getPdfProcessedResult(
        @Path("id") id: Long
    ): NetworkState<PdfFileUploadResponse>

    @Headers("Auth: true")
    @GET("api/files/{id}/result")
    suspend fun deleteVocabulary(
        @Path("id") id: Long
    ): NetworkState<PdfFileUploadResponse>

    @DELETE("api/files/{id}/cancel")
    @Headers("Auth: true")
    suspend fun cancelUpload(
        @Path("id") id: Long
    ): NetworkState<Unit>  // 204 No Content 응답
}