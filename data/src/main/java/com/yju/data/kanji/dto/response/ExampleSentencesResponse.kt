package com.yju.data.kanji.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExampleSentencesResponse(
    @SerialName("examples")
    val examples: List<ExampleSentenceDto>
)