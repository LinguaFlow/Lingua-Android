package com.yju.domain.known.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslationExampleModel (
    @SerialName("japanese")
    val japanese: String,
    @SerialName("korean")
    val korean: String
)
