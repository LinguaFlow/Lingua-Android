package com.yju.domain.kanji.util

import android.os.Parcel
import kotlinx.android.parcel.Parceler

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeParceler : Parceler<LocalDateTime> {
    // 기존 DateTimeConverters와 동일한 포맷터 사용
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun create(parcel: Parcel): LocalDateTime {
        return LocalDateTime.parse(parcel.readString()!!, formatter)
    }

    override fun LocalDateTime.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this.format(formatter))
    }
}