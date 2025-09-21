package com.yju.domain.pdf.usecase

import com.yju.domain.pdf.repository.PdfRepository
import javax.inject.Inject

class DeleteVocabularyUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) {
    suspend operator fun invoke(vocabularyUploadId: Long): Result<Unit> {
        return pdfRepository.deleteVocabulary(vocabularyUploadId)
    }
}