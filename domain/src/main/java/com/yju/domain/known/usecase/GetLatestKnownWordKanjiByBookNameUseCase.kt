package com.yju.domain.known.usecase

import com.yju.domain.known.model.KnownWordKanjiModel
import com.yju.domain.known.repository.KnownWordKanjiRepository
import javax.inject.Inject

/**
 * 특정 책 이름에 해당하는 최신 한자 모르는 단어를 가져오는 UseCase
 */
class GetLatestKnownWordKanjiByBookNameUseCase @Inject constructor(
    private val repository: KnownWordKanjiRepository
) {
    suspend operator fun invoke(bookName: String): KnownWordKanjiModel? {
        return repository.getLatestKnownKanjiByBookName(bookName)
    }
}