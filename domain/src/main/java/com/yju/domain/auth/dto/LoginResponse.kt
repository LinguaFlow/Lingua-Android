package com.yju.domain.auth.dto

import com.yju.domain.auth.model.MemberInfo
import com.yju.domain.auth.model.TokenInfo
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val tokenInfo: TokenInfo,
    val userInfo: MemberInfo,
    val message: String,
    val timestamp: String
)