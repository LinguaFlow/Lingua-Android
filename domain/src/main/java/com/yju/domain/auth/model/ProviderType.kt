package com.yju.domain.auth.model

enum class ProviderType(val providerName: String) {
    KAKAO("kakao"),
    NAVER("naver");

    companion object {
        fun fromProviderName(name: String): ProviderType {
            return ProviderType.entries.find { it.providerName.equals(name, ignoreCase = true) }
                ?: throw IllegalArgumentException("지원하지 않는 제공자입니다: $name")
        }
    }
}
