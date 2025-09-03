package com.yju.domain.known.model


import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.domain.kanji.util.LocalDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime


@Serializable
data class KnownWordKanjiModel(
    val id: Long,

    @SerialName("book_name")
    val bookName: String,

    @SerialName("file_name")
    val word: List<KanjiDetailModel>,
    @SerialName("created_at")
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    )