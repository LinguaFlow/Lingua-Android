package com.yju.data.kanji.remote

import com.yju.data.kanji.dto.response.ExampleSentencesResponse
import com.yju.domain.util.NetworkState

interface KanjiRemoteDataSource {
    suspend fun createTranslations(word: String , level: String) : NetworkState<ExampleSentencesResponse>
}