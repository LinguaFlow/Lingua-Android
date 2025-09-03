package com.yju.domain.auth.usecase

import com.yju.domain.auth.repository.AuthRepository
import javax.inject.Inject

class CheckUserExistsUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(provider: String, token: String) =
        authRepository.checkUserExists(provider, token)
}