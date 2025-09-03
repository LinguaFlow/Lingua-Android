package com.yju.domain.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class PostUserReissueEntity (
    val accessToken: String,
    val refreshToken: String
)