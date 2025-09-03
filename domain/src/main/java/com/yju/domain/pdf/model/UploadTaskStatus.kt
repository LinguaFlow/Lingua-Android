package com.yju.domain.pdf.model

enum class UploadTaskStatus(val status: String) {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    DONE("DONE"),
    FAILED("FAILED");

    companion object {
        fun from(status: String): UploadTaskStatus {
            return UploadTaskStatus.entries.find { it.status == status } ?: FAILED
        }
    }
}