package com.yju.presentation.view.login

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback

import com.yju.presentation.R
import com.yju.presentation.base.BaseActivity
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.databinding.ActivityLoginBinding
import com.yju.presentation.ext.repeatOnStarted
import com.yju.presentation.view.home.HomeActivity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import com.yju.domain.auth.util.SharedPreferenceUtil
import com.yju.presentation.BuildConfig
import com.yju.presentation.util.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding, LoginViewModel>(R.layout.activity_login) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.checkAutoLogin()) {
            navigateToHome()
            return
        }

        initializeSocialSDKs()
        setupObservers()
        setupBackPressed()
    }

    /**
     * 소셜 로그인 SDK 초기화
     */
    private fun initializeSocialSDKs() {
        NaverIdLoginSDK.initialize(
            this,
            BuildConfig.NAVER_CLIENT_ID,
            BuildConfig.NAVER_CLIENT_SECRET,
            getString(R.string.app_name)
        )

        Timber.d("Social SDKs initialized")
    }

    private fun setupObservers() {
        // 로그인 성공 관찰
        repeatOnStarted {
            viewModel.loginSuccess.collect { success ->
                if (success) {
                    navigateToHome()
                }
            }
        }

        // 카카오 로그인 클릭 관찰
        repeatOnStarted {
            viewModel.clickKakao.collect { clicked ->
                if (clicked) {
                    performKakaoLogin()
                }
            }
        }

        // 네이버 로그인 클릭 관찰
        repeatOnStarted {
            viewModel.clickNaver.collect { clicked ->
                if (clicked) {
                    performNaverLogin()
                }
            }
        }
    }

    /**
     * 뒤로가기 버튼 처리
     */
    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 앱 종료
                finish()
            }
        })
    }

    /**
     * 카카오 로그인 수행
     */
    private fun performKakaoLogin() {
        if (!NetworkUtils.isNetworkConnected(this)) {
            viewModel.baseEvent(BaseViewModel.UiEvent.Toast.Normal("네트워크 연결을 확인해주세요"))
            return
        }

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            when {
                error != null -> {
                    Timber.e(error, "Kakao login failed")
                    viewModel.baseEvent(BaseViewModel.UiEvent.Loading.Hide)

                    // 사용자 취소가 아닌 경우에만 에러 메시지 표시
                    if (error !is ClientError || error.reason != ClientErrorCause.Cancelled) {
                        viewModel.baseEvent(BaseViewModel.UiEvent.Toast.Normal(
                            "카카오 로그인에 실패했습니다"
                        ))
                    }
                }
                token != null -> {
                    fetchKakaoUserInfo(token)
                }
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                if (error != null) {
                    Timber.e(error, "KakaoTalk login failed")

                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }

                    UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                } else if (token != null) {
                    fetchKakaoUserInfo(token)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    /**
     * 카카오 사용자 정보 조회 및 로그인 처리
     */
    private fun fetchKakaoUserInfo(token: OAuthToken) {
        UserApiClient.instance.me { user, error ->
            when {
                error != null -> {
                    Timber.e(error, "Failed to fetch Kakao user info")
                    viewModel.baseEvent(BaseViewModel.UiEvent.Loading.Hide)
                    viewModel.baseEvent(BaseViewModel.UiEvent.Toast.Normal(
                        "사용자 정보를 가져올 수 없습니다"
                    ))
                }
                user != null -> {
                    val email = user.kakaoAccount?.email ?: ""
                    val nickname = user.kakaoAccount?.profile?.nickname ?: user.id.toString()

                    Timber.d("Kakao user info: email=$email, nickname=$nickname")

                    // 바로 로그인 처리
                    viewModel.performSocialLogin("kakao", token.accessToken)
                }
            }
        }
    }

    /**
     * 네이버 로그인 수행
     */
    private fun performNaverLogin() {
        if (!NetworkUtils.isNetworkConnected(this)) {
            viewModel.baseEvent(BaseViewModel.UiEvent.Toast.Normal("네트워크 연결을 확인해주세요"))
            return
        }

        val oauthLoginCallback = object : OAuthLoginCallback {
            override fun onSuccess() {
                val accessToken = NaverIdLoginSDK.getAccessToken() ?: ""
                fetchNaverUserInfo(accessToken)
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Timber.e("Naver login failed: $message")
                viewModel.baseEvent(BaseViewModel.UiEvent.Loading.Hide)
                viewModel.baseEvent(BaseViewModel.UiEvent.Toast.Normal("네이버 로그인에 실패했습니다"))
            }

            override fun onError(errorCode: Int, message: String) {
                Timber.e("Naver login error: $message")
                viewModel.baseEvent(BaseViewModel.UiEvent.Loading.Hide)
                viewModel.baseEvent(BaseViewModel.UiEvent.Toast.Normal("네이버 로그인 오류가 발생했습니다"))
            }
        }

        NaverIdLoginSDK.authenticate(this, oauthLoginCallback)
    }

    /**
     * 네이버 사용자 정보 조회 및 로그인 처리
     */
    private fun fetchNaverUserInfo(accessToken: String) {
        NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
            override fun onSuccess(response: NidProfileResponse) {
                val profile = response.profile
                val email = profile?.email ?: ""
                val nickname = profile?.nickname ?: profile?.id ?: ""

                Timber.d("Naver user info: email=$email, nickname=$nickname")

                // 바로 로그인 처리
                viewModel.performSocialLogin("naver", accessToken)
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Timber.e("Naver profile fetch failed: $message")
                viewModel.baseEvent(BaseViewModel.UiEvent.Loading.Hide)
                viewModel.baseEvent(BaseViewModel.UiEvent.Toast.Normal("네이버 프로필 정보를 가져올 수 없습니다"))
            }

            override fun onError(errorCode: Int, message: String) {
                Timber.e("Naver profile error: $message")
                viewModel.baseEvent(BaseViewModel.UiEvent.Loading.Hide)
                viewModel.baseEvent(BaseViewModel.UiEvent.Toast.Normal("네이버 프로필 오류가 발생했습니다"))
            }
        })
    }

    /**
     * 홈 화면(PdfViewerFragment)으로 이동
     */
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)

        // 화면 전환 애니메이션
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        finish()
    }
}