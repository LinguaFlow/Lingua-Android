package com.yju.domain.known.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslationExamplesModel(
    @SerialName("examples")
    val examples: List<TranslationExampleModel>
)