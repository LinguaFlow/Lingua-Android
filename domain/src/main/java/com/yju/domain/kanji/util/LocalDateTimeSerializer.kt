package com.yju.domain.kanji.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    // DateTimeConverters와 동일한 포맷터 사용
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        // DateTimeConverters의 fromLocalDataTime과 동일한 로직 사용
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        // DateTimeConverters의 toLocalDataTime과 동일한 로직 사용
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}