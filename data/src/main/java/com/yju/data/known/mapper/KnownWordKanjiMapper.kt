package com.yju.data.known.mapper

import com.yju.data.kanji.util.KanjiDetails
import com.yju.data.known.entity.KnownWordKanji
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.domain.known.model.KnownWordKanjiModel

// Kanji → KanjiModel
fun KnownWordKanji.toKnownKanjiModel(): KnownWordKanjiModel {
    val detailsList: List<KanjiDetailModel> = this.knownWord.flatMap { (_, value) ->
        when (value) {
            is List<*> -> value  // Any → List<*>
                .filterIsInstance<KanjiDetails>()  // KanjiDetails 만 추려내기
                .map { it.toKnownKanjiDetailModel() }
            else -> emptyList()
        }
    }

    return KnownWordKanjiModel(
        id = id,
        bookName = bookName,
        word = detailsList,
        createdAt = createdAt
    )
}

fun KanjiDetails.toKnownKanjiDetailModel(): KanjiDetailModel {
    return KanjiDetailModel(
        vocabularyBookOrder = this.vocabularyBookOrder,
        kanji = this.kanji,
        furigana = this.furigana,
        means = this.means,
        level = this.level,
        page = this.page
    )
}

fun KanjiDetailModel.toKanjiDetails(): KanjiDetails {
    return KanjiDetails(
        vocabularyBookOrder = this.vocabularyBookOrder,
        kanji = this.kanji,
        furigana = this.furigana,
        means = this.means,
        level = this.level,
        page = this.page
    )
}