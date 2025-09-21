package com.yju.data.pdf.remote

import android.util.Log
import com.yju.data.pdf.mapper.toPdfModel
import com.yju.data.pdf.util.RetrofitFailureStateException
import com.yju.domain.pdf.model.PdfModel
import com.yju.domain.pdf.model.UploadTaskStatus
import com.yju.domain.pdf.repository.PdfRepository
import com.yju.domain.util.NetworkState

import okhttp3.MultipartBody
import javax.inject.Inject

class PdfRepositoryImpl @Inject constructor(
    private val remoteDataSource: PdfRemoteDataSource
) : PdfRepository {
    // 단어장 PDF 업로드 후 ID 반환
    override suspend fun uploadVocabularyPdf(filePart: MultipartBody.Part): Result<Long> {
        return when (val response = remoteDataSource.uploadVocabularyPdf(filePart)) {
            is NetworkState.Success -> {
                val taskId = response.body.taskId
                Result.success(taskId)
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
    override suspend fun getPdfProcessingStatus(id: Long): Result<UploadTaskStatus> {
        return when (val response = remoteDataSource.getPdfProcessingStatus(id)) {
            is NetworkState.Success -> {
                val status = UploadTaskStatus.from(response.body.status)
                Result.success(status)
            }
            is NetworkState.Failure -> Result.failure(RetrofitFailureStateException(response.message, response.code))
            is NetworkState.NetworkError -> Result.failure(IllegalStateException("네트워크 에러"))
            is NetworkState.UnknownError -> Result.failure(IllegalStateException("알 수 없는 에러"))
        }
    }

    // 처리 완료된 PDF 결과 조회
    override suspend fun getPdfProcessedResult(id: Long): Result<PdfModel> {
        return when (val response = remoteDataSource.getPdfProcessedResult(id)) {
            is NetworkState.Success -> {
                try {
                    val result = response.body.toPdfModel()
                    Result.success(result)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            is NetworkState.Failure -> {
                Result.failure(RetrofitFailureStateException(response.message, response.code))
            }
            is NetworkState.NetworkError -> {
                Result.failure(IllegalStateException("네트워크 에러"))
            }
            is NetworkState.UnknownError -> {
                Result.failure(IllegalStateException("알 수 없는 에러"))
            }
        }
    }

    override suspend fun deleteVocabulary(vocabularyUploadId: Long): Result<Unit> {
        TODO("Not yet implemented")
    }


    override suspend fun cancelUpload(id: Long): Result<Unit> {
        return when (val response = remoteDataSource.cancelUpload(id)) {
            is NetworkState.Success -> Result.success(Unit)
            is NetworkState.Failure -> Result.failure(
                RetrofitFailureStateException(response.message, response.code)
            )
            is NetworkState.NetworkError -> Result.failure(
                IllegalStateException("네트워크 오류")
            )
            is NetworkState.UnknownError -> Result.failure(
                IllegalStateException("알 수 없는 오류")
            )
        }
    }
}