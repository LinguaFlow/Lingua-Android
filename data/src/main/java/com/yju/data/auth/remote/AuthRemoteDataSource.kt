package com.yju.data.auth.remote

import com.yju.domain.auth.dto.LoginResponse
import com.yju.domain.auth.model.ProviderType
import com.yju.domain.auth.model.TokenInfo

interface AuthRemoteDataSource {
    suspend fun socialLogin(provider: ProviderType, accessToken: String): LoginResponse
    suspend fun reissueToken(refreshToken: String): TokenInfo
    suspend fun logout(refreshToken: String)
}