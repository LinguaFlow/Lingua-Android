package com.yju.domain.auth.util

import com.yju.domain.BuildConfig
import com.yju.domain.auth.model.TokenInfo
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val prefs: SharedPreferenceUtil
) : okhttp3.Authenticator {

    // 세션 만료 상태 관리 (스레드 안전)
    private val isSessionExpired = AtomicBoolean(false)

    override fun authenticate(route: Route?, response: Response): Request? {
        Timber.d("============ AuthInterceptor 호출됨 ============")
        Timber.d("응답 코드: ${response.code}")
        Timber.d("요청 URL: ${response.request.url}")

        val originRequest = response.request

        // Authorization 헤더 체크
        val authHeader = originRequest.header("Authorization")
        Timber.d("Authorization 헤더: $authHeader")

        if (authHeader.isNullOrEmpty()) {
            Timber.d("Authorization 헤더가 없음 - 재시도 안함")
            return null
        }

        // 이미 재시도한 요청인 경우 null 반환 (무한 루프 방지)
        if (originRequest.header("Authorization-Retry") != null) {
            Timber.d("이미 재시도한 요청 - 무한 루프 방지")
            return null
        }

        // 세션 만료 상태 체크
        if (isSessionExpired.get()) {
            Timber.d("세션이 이미 만료됨")
            return null
        }

        // 401 응답 처리
        if (response.code == 401) {
            Timber.d("401 응답 - 토큰 재발급 시도")

            try {
                val refreshRequest = createRefreshTokenRequest()
                val refreshedToken = executeRefreshTokenRequest(refreshRequest)

                return refreshedToken?.let {
                    updateTokenInPrefs(it.accessToken, it.refreshToken)
                    Timber.d("토큰 재발급 성공 - 새 액세스 토큰으로 재시도")

                    // 원래 요청을 새 토큰으로 재시도
                    originRequest.newBuilder()
                        .removeHeader("Authorization")
                        .header("Authorization", "Bearer ${it.accessToken}")
                        .header("Authorization-Retry", "true")
                        .build()
                } ?: run {
                    Timber.e("토큰 재발급 실패 - null 반환")
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "토큰 재발급 중 예외 발생")
                clearTokens()
                return null
            }
        }

        return null
    }

    private fun createRefreshTokenRequest(): Request {
        val refreshToken = prefs.getString("refreshToken", "")

        val requestBody = refreshToken

        Timber.d("토큰 재발급 요청 생성")
        Timber.d("Refresh Token 길이: ${refreshToken.length}")

        return Request.Builder()
//            .url("${BuildConfig.BASE_URL}api/v1/auth/reissue-token")
            .url("http://192.168.1.101:8080/api/v1/auth/reissue-token")
            .post(requestBody.toRequestBody("text/plain".toMediaTypeOrNull()))
            .build()
    }

    private fun executeRefreshTokenRequest(refreshRequest: Request): TokenInfo? {
        return try {
            val response = OkHttpClient().newCall(refreshRequest).execute()
            response.use { res ->
                Timber.d("토큰 재발급 응답 코드: ${res.code}")

                when {
                    res.isSuccessful -> {
                        val json = Json {
                            ignoreUnknownKeys = true
                            coerceInputValues = true
                        }
                        val responseBody = res.body?.string()

                        responseBody?.let {
                            Timber.d("토큰 재발급 응답: $it")
                            try {
                                val tokenInfo = json.decodeFromString<TokenInfo>(it)
                                Timber.d("새 액세스 토큰 받음")
                                tokenInfo
                            } catch (e: Exception) {
                                Timber.e(e, "토큰 파싱 실패")
                                null
                            }
                        } ?: run {
                            Timber.e("응답 바디가 비어있음")
                            clearTokens()
                            null
                        }
                    }
                    res.code == 401 -> {
                        Timber.e("Refresh Token 만료 - 로그인 필요")
                        isSessionExpired.set(true)
                        clearTokens()
                        null
                    }
                    else -> {
                        Timber.e("토큰 재발급 실패: ${res.code} ${res.message}")
                        val errorBody = res.body?.string()
                        Timber.e("에러 응답: $errorBody")
                        clearTokens()
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "토큰 재발급 중 네트워크 예외 발생")
            clearTokens()
            null
        }
    }

    private fun updateTokenInPrefs(accessToken: String, refreshToken: String) {
        prefs.setString("accessToken", accessToken)
        prefs.setString("refreshToken", refreshToken)
        prefs.setString("tokenCreateTime", System.currentTimeMillis().toString())
    }

    private fun clearTokens() {
        prefs.removeString("accessToken")
        prefs.removeString("refreshToken")
        prefs.removeString("tokenCreateTime")
        prefs.removeString("loginMethod")
        isSessionExpired.set(true)
    }

    fun resetSessionExpiredFlag() {
        isSessionExpired.set(false)
        Timber.d("세션 만료 플래그 리셋")
    }

    fun isSessionExpired(): Boolean {
        return isSessionExpired.get()
    }

    fun logout() {
        clearTokens()
        Timber.d("수동 로그아웃 처리")
    }
}