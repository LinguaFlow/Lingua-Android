package com.yju.domain.auth.usecase

import com.yju.domain.auth.dto.LoginResponse
import com.yju.domain.auth.model.ProviderType
import com.yju.domain.auth.repository.AuthRepository
import javax.inject.Inject

class SocialLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(provider: String, accessToken: String): Result<LoginResponse> {

        val providerType = ProviderType.fromProviderName(provider)

        return authRepository.socialLogin(providerType, accessToken)
    }
}
