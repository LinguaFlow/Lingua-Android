package com.yju.domain.pdf.usecase.save

import com.yju.domain.pdf.model.PdfUploadModel
import com.yju.domain.pdf.repository.PdfRepository
import okhttp3.MultipartBody
import javax.inject.Inject

class PdfJoinUseCase @Inject constructor(private val pdfRepository: PdfRepository) {
    suspend operator fun invoke(filePart: MultipartBody.Part): Result<PdfUploadModel> {
        // Don't wrap the result again - just return it directly
        return pdfRepository.uploadPdfFile(filePart)
    }
}