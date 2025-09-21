package com.yju.domain.pdf.usecase

import com.yju.domain.pdf.repository.PdfRepository
import javax.inject.Inject

class CancelUploadUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) {
    suspend operator fun invoke(taskId: Long): Result<Unit> {
        return pdfRepository.cancelUpload(taskId)
    }
}