package com.yju.domain.pdf.usecase

import com.yju.domain.pdf.model.PdfModel
import com.yju.domain.pdf.repository.PdfRepository
import okhttp3.MultipartBody
import javax.inject.Inject

class UploadPdfUseCase @Inject constructor(private val pdfRepository: PdfRepository) {
    suspend operator fun invoke(filePart: MultipartBody.Part): Result<PdfModel> {
        // Don't wrap the result again - just return it directly
        return pdfRepository.uploadPdfFileForTest(filePart)
    }
}