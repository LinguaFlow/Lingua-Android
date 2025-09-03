package com.yju.domain.auth.model

data class SocialUserModel(
    val provider: String,
    val token: String,
    val email: String,
    val nickname: String
)