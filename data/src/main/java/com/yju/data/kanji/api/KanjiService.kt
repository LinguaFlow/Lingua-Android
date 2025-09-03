package com.yju.data.kanji.api

import com.yju.data.kanji.dto.request.ExampleSentenceRequest
import com.yju.data.kanji.dto.response.ExampleSentencesResponse
import com.yju.domain.util.NetworkState
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface KanjiService {
    @POST("examples")
    @Headers("Auth: true")
    suspend fun createTranslations(
        @Body request: ExampleSentenceRequest
    ): NetworkState<ExampleSentencesResponse>
}