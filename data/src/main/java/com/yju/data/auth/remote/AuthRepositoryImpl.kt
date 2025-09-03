package com.yju.data.auth.remote

import com.yju.domain.auth.dto.LoginResponse
import com.yju.domain.auth.model.ProviderType
import com.yju.domain.auth.model.TokenInfo
import com.yju.domain.auth.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource
) : AuthRepository {

    override suspend fun socialLogin(
        provider: ProviderType,
        accessToken: String
    ): Result<LoginResponse> {
        return try {
            val response = authRemoteDataSource.socialLogin(provider, accessToken)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reissueToken(refreshToken: String): Result<TokenInfo> {
        return try {
            val response = authRemoteDataSource.reissueToken(refreshToken)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(refreshToken: String): Result<Unit> {
        return try {
            authRemoteDataSource.logout(refreshToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkUserExists(provider: String, token: String): Result<Boolean> {
        return try {
            // 실제 백엔드 API 호출하여 사용자 존재 여부 확인
            // 임시로 소셜 로그인 시도해보고 성공하면 true, 실패하면 false
            val providerType = ProviderType.fromProviderName(provider)
            val response = authRemoteDataSource.socialLogin(providerType, token)
            Result.success(true)
        } catch (e: Exception) {
            // 404 또는 사용자 없음 에러인 경우 false 반환
            if (e.message?.contains("404") == true ||
                e.message?.contains("사용자를 찾을 수 없습니다") == true) {
                Result.success(false)
            } else {
                Result.failure(e)
            }
        }
    }
}