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
        Log.d("PdfDebug", "결과 조회 시작 - ID: $id")
        return when (val response = remoteDataSource.getPdfProcessedResult(id)) {
            is NetworkState.Success -> {
                Log.d("PdfDebug", "서버 응답 성공 - 매핑 시작")
                try {
                    val result = response.body.toPdfModel()
                    Log.d("PdfDebug", "매핑 성공")
                    Result.success(result)
                } catch (e: Exception) {
                    Log.e("PdfDebug", "매핑 실패: ${e.message}", e)
                    Result.failure(e)
                }
            }
            is NetworkState.Failure -> {
                Log.e("PdfDebug", "네트워크 실패: ${response.message}")
                Result.failure(RetrofitFailureStateException(response.message, response.code))
            }
            is NetworkState.NetworkError -> {
                Log.e("PdfDebug", "네트워크 에러")
                Result.failure(IllegalStateException("네트워크 에러"))
            }
            is NetworkState.UnknownError -> {
                Log.e("PdfDebug", "알 수 없는 에러")
                Result.failure(IllegalStateException("알 수 없는 에러"))
            }
        }
    }
}