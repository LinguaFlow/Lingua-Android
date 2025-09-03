package com.yju.domain.util

import com.yju.domain.pdf.model.PdfModel
import com.yju.domain.pdf.model.UploadTaskStatus

sealed class AsyncUploadState {
    object Uploading : AsyncUploadState()
    data class Submitted(val taskId: Long) : AsyncUploadState()
    data class Processing(val status: UploadTaskStatus) : AsyncUploadState()
    data class Success(val result: PdfModel) : AsyncUploadState()
    data class Error(val error: Throwable) : AsyncUploadState()
}