package com.yju.domain.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class TokenReissueResponse(
    val accessToken: String,
    val refreshToken: String
)
