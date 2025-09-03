package com.yju.domain.auth.usecase

import com.yju.domain.auth.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(refreshToken: String) =
        authRepository.logout(refreshToken)
}
