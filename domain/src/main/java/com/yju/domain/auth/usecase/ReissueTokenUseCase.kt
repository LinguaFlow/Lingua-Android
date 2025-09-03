package com.yju.domain.auth.usecase

import com.yju.domain.auth.repository.AuthRepository
import javax.inject.Inject

class ReissueTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(refreshToken: String) =
        authRepository.reissueToken(refreshToken)
}
