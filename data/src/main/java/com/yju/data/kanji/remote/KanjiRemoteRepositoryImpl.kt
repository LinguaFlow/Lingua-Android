package com.yju.data.kanji.remote

import com.yju.data.kanji.mapper.toTranslationExamplesModel
import com.yju.data.pdf.util.RetrofitFailureStateException
import com.yju.domain.known.model.TranslationExamplesModel
import com.yju.domain.known.repository.KanjiRemoteRepository
import com.yju.domain.util.NetworkState
import javax.inject.Inject

class KanjiRemoteRepositoryImpl @Inject constructor(
    private val remoteDataSource: KanjiRemoteDataSource
) : KanjiRemoteRepository {

    override suspend fun getTranslationExamples(
        word: String,
        level: String
    ): Result<TranslationExamplesModel> {
        return when (val response = remoteDataSource.createTranslations(word, level)) {
            is NetworkState.Success -> {
                // 단일 객체에 대한 처리
                val model = response.body.toTranslationExamplesModel()  // ← Unresolved reference!

                Result.success(model)
            }
            // 나머지 코드는 그대로
            is NetworkState.Failure -> Result.failure(
                RetrofitFailureStateException(
                    response.message,
                    response.code
                )
            )
            is NetworkState.NetworkError -> Result.failure(
                IllegalStateException("네트워크 에러")
            )
            is NetworkState.UnknownError -> Result.failure(
                IllegalStateException("알 수 없는 에러")
            )
        }
    }
}