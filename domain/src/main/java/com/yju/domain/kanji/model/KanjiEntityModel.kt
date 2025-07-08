package com.yju.domain.kanji.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime


data class KanjiEntityModel (
    val id: Long? = null,

    val data: Map<String, List<Any>>,

    val createdAt: LocalDateTime? = null,

    val updatedAt: LocalDateTime? = null,

    val deletedAt: LocalDateTime? = null
)