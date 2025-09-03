package com.yju.domain.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class TokenInfo(
    val accessToken: String,
    val accessTokenExpiresIn: Long,
    val refreshToken: String,
    val refreshTokenExpiresIn: Long,
    val tokenType: String = "Bearer"
)