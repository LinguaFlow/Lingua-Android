package com.yju.presentation.view.login


import android.media.metrics.Event
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.MutableEventFlow
import com.yju.domain.auth.model.SocialUserModel
import com.yju.domain.auth.usecase.CheckUserExistsUseCase
import com.yju.domain.auth.usecase.SocialLoginUseCase
import com.yju.domain.auth.util.AuthInterceptor

import com.yju.domain.auth.util.SharedPreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val prefs: SharedPreferenceUtil,
    private val socialLoginUseCase: SocialLoginUseCase,
    private val authInterceptor: AuthInterceptor
) : BaseViewModel() {

    // 로그인 성공 이벤트
    private val _loginSuccess = MutableEventFlow<Boolean>()
    val loginSuccess: EventFlow<Boolean> get() = _loginSuccess

    // 카카오 로그인 클릭
    private val _clickKakao = MutableEventFlow<Boolean>()
    val clickKakao: EventFlow<Boolean> get() = _clickKakao

    // 네이버 로그인 클릭
    private val _clickNaver = MutableEventFlow<Boolean>()
    val clickNaver: EventFlow<Boolean> get() = _clickNaver

    // 소셜 로그인 임시 데이터
    private var tempSocialData: SocialUserModel? = null

    /**
     * 소셜 로그인 수행
     */
    fun performSocialLogin(provider: String, token: String) {
        viewModelScope.launch {
            socialLoginUseCase(provider, token).onSuccess {
                    // 토큰 저장 (토큰 생성 시간 추가)
                    prefs.setString("accessToken", it.tokenInfo.accessToken)
                    prefs.setString("refreshToken", it.tokenInfo.refreshToken)
                    prefs.setString("loginMethod", provider)
                    prefs.setString("tokenCreateTime", System.currentTimeMillis().toString()) // 추가!
                    // 사용자 정보 저장
                    prefs.setString("userEmail", it.userInfo.email)
                    prefs.setString("userName", it.userInfo.name)
                    prefs.setString("userRole", it.userInfo.role)

                    // 인터셉터 초기화
                    authInterceptor.resetSessionExpiredFlag()

                    // 디버깅용 로그 추가
                    Timber.d("토큰 저장 완료 - AccessToken 길이: ${it.tokenInfo.accessToken.length}")
                    Timber.d("토큰 생성 시간 저장: ${System.currentTimeMillis()}")

                    baseEvent(UiEvent.Loading.Hide)
                    baseEvent(UiEvent.Toast.Success("로그인 성공"))

                    // 로그인 성공 이벤트 발생
                    _loginSuccess.emit(true)
                }
                .onFailure { error ->
                    baseEvent(UiEvent.Loading.Hide)

                    val errorMessage = when {
                        error.message?.contains("404") == true ->
                            "등록되지 않은 사용자입니다. 관리자에게 문의하세요."
                        error.message?.contains("Network") == true ->
                            "네트워크 연결을 확인해주세요."
                        else ->
                            error.message ?: "로그인에 실패했습니다."
                    }

                    baseEvent(UiEvent.Toast.Normal(errorMessage))
                    Timber.e(error, "Social login failed")
                }
        }
    }

    fun setSocialTempData(provider: String, token: String, email: String, nickname: String) {
        tempSocialData = SocialUserModel(
            provider = provider,
            token = token,
            email = email,
            nickname = nickname
        )
        Timber.d("Social temp data saved: provider=$provider, email=$email")
    }

    /**
     * 소셜 데이터 가져오기
     */
    fun getSocialTempData(): SocialUserModel? = tempSocialData

    /**
     * 카카오 로그인 클릭
     */
    fun onClickKakaoLogin() {
        viewModelScope.launch {
            _clickKakao.emit(true)
        }
    }

    /**
     * 네이버 로그인 클릭
     */
    fun onClickNaverLogin() {
        viewModelScope.launch {
            _clickNaver.emit(true)
        }
    }

    /**
     * 자동 로그인 체크
     */
    fun checkAutoLogin(): Boolean {
        val accessToken = prefs.getString("accessToken", "")
        val refreshToken = prefs.getString("refreshToken", "")

        return accessToken.isNotEmpty() && refreshToken.isNotEmpty()
    }

    /**
     * 로그아웃
     */
    fun logout() {
        // 저장된 토큰 및 사용자 정보 삭제
        prefs.removeString("accessToken")
        prefs.removeString("refreshToken")
        prefs.removeString("loginMethod")
        prefs.removeString("tokenCreateTime") // 추가!
        prefs.removeString("userEmail")
        prefs.removeString("userName")
        prefs.removeString("userRole")

        // 소셜 SDK 로그아웃
        UserApiClient.instance.logout {}
        NaverIdLoginSDK.logout()

        Timber.d("User logged out")
    }
}