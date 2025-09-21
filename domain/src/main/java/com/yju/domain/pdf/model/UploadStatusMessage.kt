package com.yju.domain.pdf.model

import kotlinx.serialization.Serializable

@Serializable
data class UploadStatusMessage(
    val taskId: Long,
    val status: String,
    val progress: Int? = null,
    val message: String? = null
)