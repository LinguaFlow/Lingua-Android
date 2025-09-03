package com.yju.data.kanji.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExampleSentenceRequest(
    @SerialName("word")
    val word: String,
    @SerialName("level")
    val level: String
)