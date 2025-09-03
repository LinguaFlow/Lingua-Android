package com.yju.domain.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class MemberInfo(
    val id: Long,
    val email: String,
    val name: String,
    val picture: String? = null,
    val role: String,
    val provider: String
)