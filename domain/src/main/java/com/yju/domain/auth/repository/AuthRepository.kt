package com.yju.domain.auth.repository

import com.yju.domain.auth.dto.LoginResponse
import com.yju.domain.auth.model.ProviderType
import com.yju.domain.auth.model.TokenInfo

interface AuthRepository {
    // 소셜 로그인
    suspend fun socialLogin(provider: ProviderType, accessToken: String): Result<LoginResponse>

    // 토큰 재발급
    suspend fun reissueToken(refreshToken: String): Result<TokenInfo>

    // 로그아웃
    suspend fun logout(refreshToken: String): Result<Unit>

    // 회원가입 여부 확인
    suspend fun checkUserExists(provider: String, token: String): Result<Boolean>
}