package com.yju.data.pdf.remote

import com.yju.data.pdf.mapper.toPdfModel
import com.yju.data.pdf.util.RetrofitFailureStateException
import com.yju.domain.pdf.model.PdfModel
import com.yju.domain.pdf.model.UploadTaskStatus
import com.yju.domain.util.NetworkState

import okhttp3.MultipartBody
import javax.inject.Inject

class PdfRepositoryImpl @Inject constructor(
    private val remoteDataSource: PdfRemoteDataSource
) : PdfRepository {
    // 단어장 PDF 업로드 후 ID 반환
    override suspend fun uploadVocabularyPdf(filePart: MultipartBody.Part): Result<String> {
        return when (val response = remoteDataSource.uploadVocabularyPdf(filePart)) {
            is NetworkState.Success -> {
                val id = response.body["id"] ?: ""
                if (id.isNotEmpty()) {
                    Result.success(id)
                } else {
                    Result.failure(IllegalArgumentException("ID가 없습니다"))
                }
            }

            is NetworkState.Failure -> Result.failure(
                RetrofitFailureStateException(
                    response.message,
                    response.code
                )
            )

            is NetworkState.NetworkError -> Result.failure(IllegalStateException("네트워크 에러"))
            is NetworkState.UnknownError -> Result.failure(IllegalStateException("알 수 없는 에러"))
        }
    }

    // PDF 처리 상태 확인
    override suspend fun getPdfProcessingStatus(id: String): Result<UploadTaskStatus> {
        return when (val response = remoteDataSource.getPdfProcessingStatus(id)) {
            is NetworkState.Success -> {
                val status = UploadTaskStatus.from(response.body.status, response.body.code)
                Result.success(status)
            }
            is NetworkState.Failure -> Result.failure(RetrofitFailureStateException(response.message, response.code))
            is NetworkState.NetworkError -> Result.failure(IllegalStateException("네트워크 에러"))
            is NetworkState.UnknownError -> Result.failure(IllegalStateException("알 수 없는 에러"))
        }
    }

    // 처리 완료된 PDF 결과 조회
    override suspend fun getPdfProcessedResult(id: String): Result<PdfModel> {
        return when (val response = remoteDataSource.getPdfProcessedResult(id)) {
            is NetworkState.Success -> Result.success(response.body.toPdfModel())
            is NetworkState.Failure -> Result.failure(RetrofitFailureStateException(response.message, response.code))
            is NetworkState.NetworkError -> Result.failure(IllegalStateException("네트워크 에러"))
            is NetworkState.UnknownError -> Result.failure(IllegalStateException("알 수 없는 에러"))
        }
    }

    // 테스트용 메소드
    override suspend fun uploadPdfFileForTest(filePart: MultipartBody.Part): Result<PdfModel> {
        return when (val data = remoteDataSource.uploadTestPdf(filePart)) {
            is NetworkState.Success -> Result.success(data.body.toPdfModel())
            is NetworkState.Failure -> Result.failure(RetrofitFailureStateException(data.message, data.code))
            is NetworkState.NetworkError -> Result.failure(IllegalStateException("NetworkError"))
            is NetworkState.UnknownError -> Result.failure(IllegalStateException("UnknownError"))
        }
    }
}