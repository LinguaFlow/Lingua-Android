package com.yju.data.kanji.mapper

import com.google.gson.Gson
import com.yju.data.kanji.dto.response.ExampleSentenceDto
import com.yju.data.kanji.dto.response.ExampleSentencesResponse
import com.yju.data.kanji.entity.Kanji
import com.yju.data.kanji.util.KanjiDetails
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.domain.kanji.model.KanjiModel
import com.yju.domain.known.model.TranslationExampleModel
import com.yju.domain.known.model.TranslationExamplesModel
import com.yju.domain.pdf.model.PdfKanjiDetailModel
import com.yju.domain.pdf.model.PdfModel

// Kanji → KanjiModel
fun Kanji.toKanjiModel(): KanjiModel {
    // word의 모든 값들을 단일 리스트로 변환
    val detailsList: List<KanjiDetailModel> = this.word.flatMap { (_, value) ->
        when (value) {
            is List<*> -> {
                // KanjiDetails 타입 체크 후 변환
                value.mapNotNull { item ->
                    when (item) {
                        is KanjiDetails -> item.toKanjiDetailModel()
                        is Map<*, *> -> {
                            // Gson이 Map으로 역직렬화한 경우 처리
                            try {
                                val gson = Gson()
                                val json = gson.toJson(item)
                                val kanjiDetail = gson.fromJson(json, KanjiDetails::class.java)
                                kanjiDetail.toKanjiDetailModel()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        else -> null
                    }
                }
            }
            else -> emptyList()
        }
    }

    return KanjiModel(
        id = id,
        bookName = bookName,
        word = detailsList,
        createdAt = createdAt
    )
}

// KanjiDetails -> KanjiDetailModel 변환
fun KanjiDetails.toKanjiDetailModel(): KanjiDetailModel {
    return KanjiDetailModel(
        vocabularyBookOrder = this.vocabularyBookOrder,
        kanji = this.kanji,
        furigana = this.furigana,
        means = this.means,
        level = this.level,
        page = this.page
    )
}

// 단일 TranslationExample DTO를 도메인 모델로 변환
fun ExampleSentencesResponse.toTranslationExamplesModel(): TranslationExamplesModel {
    return TranslationExamplesModel(
        examples = this.examples.map { it.toTranslationExampleModel() }
    )
}

// TranslationExample을 위한 매핑 함수
fun ExampleSentenceDto.toTranslationExampleModel(): TranslationExampleModel {
    return TranslationExampleModel(
        japanese = this.japaneseExample,
        korean = this.koreanTranslation.ifEmpty { "번역 데이터가 없습니다." } // 빈 문자열 처리
    )
}


fun PdfModel.toKanji(): Kanji = Kanji(
    bookName = this.bookName, // bookName 필드 추가
    word = toKanjiMap() // 수정된 toKanjiMap() 사용
)

// 여기가 수정된 부분 - 고정 키 "items"를 사용
fun PdfModel.toKanjiMap(): Map<String, Any> =
    mapOf("items" to word.map { it.toKanjiDetails() })

fun PdfKanjiDetailModel.toKanjiDetails(): KanjiDetails = KanjiDetails(
    vocabularyBookOrder = this.vocabularyBookOrder,
    kanji = this.kanji,
    furigana = this.furigana,
    means = this.means,
    level = this.level,
    page = this.page
)
