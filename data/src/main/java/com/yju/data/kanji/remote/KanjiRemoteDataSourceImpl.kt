package com.yju.data.kanji.remote

import android.util.Log
import com.yju.data.kanji.api.KanjiService
import com.yju.data.kanji.dto.request.ExampleSentenceRequest
import com.yju.data.kanji.dto.response.ExampleSentencesResponse
import com.yju.domain.util.NetworkState
import javax.inject.Inject

class KanjiRemoteDataSourceImpl @Inject constructor (
    private val kanjiService: KanjiService
) : KanjiRemoteDataSource {
    override suspend fun createTranslations(
        word: String,
        level: String
    ): NetworkState<ExampleSentencesResponse> {
        val request = ExampleSentenceRequest(word, level)
        Log.d("KanjiRemote", "요청 데이터: word=$word, level=$level")

        // Assuming kanjiService.createTranslations now returns a single object
        return kanjiService.createTranslations(request)
    }
}