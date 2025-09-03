package com.yju.domain.kanji.model


import android.os.Parcelable
import com.yju.domain.kanji.util.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime


@Parcelize
@Serializable
data class KanjiModel(
    val id: Long,

    @SerialName("book_name")
    val bookName: String,

    @SerialName("file_name")
    val word: List<KanjiDetailModel>,

    @SerialName("created_at")
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    ) : Parcelable