package com.yju.data.pdf.remote

import com.yju.data.pdf.api.PdfService
import com.yju.data.pdf.dto.response.PdfFileUploadResponse
import okhttp3.MultipartBody
import javax.inject.Inject

class PdfRemoteDataSourceImpl @Inject constructor(
    private val pdfService: PdfService
) : PdfRemoteDataSource {

    override suspend fun uploadPdfFile(filePart: MultipartBody.Part): Result<PdfFileUploadResponse> {
        return try {
            val response = pdfService.uploadPdfFile(filePart)

            when (response.isSuccessful) {
                true -> {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body)
                    } else {
                        Result.failure(IllegalStateException("Response body is null"))
                    }
                }
                false -> {
                    val errorMsg = response.errorBody()?.string()
                    val statusCode = response.code()
                    Result.failure(Throwable("Error code=$statusCode, msg=$errorMsg"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
