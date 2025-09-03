package com.yju.data.kanji.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExampleSentenceDto(
    @SerialName("japanese_example")
    val japaneseExample: String,
    @SerialName("korean_translation")
    val koreanTranslation: String
)