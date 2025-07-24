package com.yju.data.pdf.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    @SerialName("status") val status: String,
    @SerialName("code")   val code: String
)