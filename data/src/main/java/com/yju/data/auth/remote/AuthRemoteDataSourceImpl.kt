package com.yju.data.auth.remote

import com.yju.data.auth.api.AuthService
import com.yju.domain.auth.dto.LoginResponse
import com.yju.domain.auth.model.ProviderType
import com.yju.domain.auth.model.TokenInfo
import com.yju.domain.auth.util.SharedPreferenceUtil
import javax.inject.Inject

class AuthRemoteDataSourceImpl @Inject constructor(
    private val authService: AuthService
) : AuthRemoteDataSource {

    override suspend fun socialLogin(
        provider: ProviderType,
        accessToken: String
    ): LoginResponse {
        return authService.socialLogin(provider.providerName, accessToken)
    }

    override suspend fun reissueToken(refreshToken: String): TokenInfo {
        return authService.reissueToken(refreshToken)
    }

    override suspend fun logout(refreshToken: String) {
        authService.logout(refreshToken)
    }
}