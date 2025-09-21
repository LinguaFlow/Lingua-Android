package com.yju.data.pdf.dto.response

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class FileUploadResponse(
    @SerializedName("taskId")
    val taskId: Long,

    @SerializedName("fileName")
    val fileName: String,

    @SerializedName("status")
    val status: String
) {
    val webSocketChannel: String
        get() = "/topic/upload/$taskId"
}