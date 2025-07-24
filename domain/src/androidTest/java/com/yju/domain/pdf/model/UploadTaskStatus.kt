package com.yju.domain.pdf.model

enum class UploadTaskStatus(val status: String, val code: String) {
    PENDING("PENDING", "1"),
    PROCESSING("PROCESSING", "2"),
    DONE("DONE", "3"),
    FAILED("FAILED", "4");

    companion object {
        fun from(status: String, code: String): UploadTaskStatus {
            return UploadTaskStatus.entries.find { it.status == status } ?: FAILED
        }
    }
}