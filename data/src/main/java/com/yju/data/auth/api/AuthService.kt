package com.yju.data.auth.api

import com.yju.domain.auth.dto.LoginResponse
import com.yju.domain.auth.model.TokenInfo
import retrofit2.http.*

interface AuthService {

    @POST("/api/v1/auth/login/{provider}")
    @Headers("Auth: false")
    suspend fun socialLogin(
        @Path("provider") provider: String,
        @Body accessToken: String
    ): LoginResponse

    @POST("/api/v1/auth/reissue-token")
    @Headers("Auth: false")
    suspend fun reissueToken(
        @Body refreshToken: String
    ): TokenInfo

    @POST("/api/v1/auth/logout")
    @Headers("Auth: false")
    suspend fun logout(
        @Header("Refresh-Token") refreshToken: String
    )
}