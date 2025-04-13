package com.yju.data.pdf.remote

import com.yju.domain.pdf.model.PdfUploadModel
import com.yju.domain.pdf.repository.PdfRepository

import okhttp3.MultipartBody
import javax.inject.Inject

class PdfRepositoryImpl @Inject constructor(
    private val remoteDataSource: PdfRemoteDataSource
) : PdfRepository {

    override suspend fun uploadPdfFile(filePart: MultipartBody.Part): Result<PdfUploadModel> {
        return remoteDataSource.uploadPdfFile(filePart).map { response ->
            PdfUploadModel(
                id = response.id,
                number = response.number,
                kanji = response.kanji,
                furigana = response.furigana,
                means = response.means,
                level = response.level
            )
        }
    }
}