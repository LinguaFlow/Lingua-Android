package com.yju.presentation.view.pdf.sheet

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.MutableEventFlow
import com.yju.presentation.util.asEventFlow
import com.yju.domain.auth.util.SharedPreferenceUtil
import com.yju.domain.auth.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuDrawerViewModel @Inject constructor(
    private val prefs: SharedPreferenceUtil,
    private val logoutUseCase: LogoutUseCase
) : BaseViewModel() {

    companion object {
        private const val TAG = "MenuDrawerViewModel"
    }

    private val _navigateToPdfManager = MutableEventFlow<Unit>()
    val navigateToPdfManager: EventFlow<Unit> = _navigateToPdfManager.asEventFlow()

    private val _navigateToVocabulary = MutableEventFlow<Unit>()
    val navigateToVocabulary: EventFlow<Unit> = _navigateToVocabulary.asEventFlow()

    private val _navigateToSettings = MutableEventFlow<Unit>()
    val navigateToSettings: EventFlow<Unit> = _navigateToSettings.asEventFlow()

    private val _logoutResult = MutableEventFlow<Result<Unit>>()
    val logoutResult: EventFlow<Result<Unit>> = _logoutResult.asEventFlow()

    private val _navigateToLogin = MutableEventFlow<Unit>()
    val navigateToLogin: EventFlow<Unit> = _navigateToLogin.asEventFlow()

    fun onPdfManagerClick() {
        viewModelScope.launch {
            _navigateToPdfManager.emit(Unit)
        }
    }

    fun onVocabularyClick() {
        viewModelScope.launch {
            _navigateToVocabulary.emit(Unit)
        }
    }

    fun onSettingsClick() {
        viewModelScope.launch {
            _navigateToSettings.emit(Unit)
        }
    }

    fun performLogout() {
        Log.d(TAG, "=== ViewModel 로그아웃 시작 ===")

        viewModelScope.launch {
            try {
                baseEvent(UiEvent.Loading.Show)

                val refreshToken = prefs.getString("refreshToken", "")
                val accessToken = prefs.getString("accessToken", "")

                Log.d(TAG, "토큰 정보:")
                Log.d(TAG, "- RefreshToken 존재: ${refreshToken.isNotEmpty()}")
                Log.d(TAG, "- AccessToken 존재: ${accessToken.isNotEmpty()}")

                if (refreshToken.isNotEmpty()) {
                    Log.d(TAG, "서버 로그아웃 API 호출 시작...")

                    val result = logoutUseCase(refreshToken)
                    result
                        .onSuccess {
                            Log.d(TAG, "서버 로그아웃 성공!")
                        }
                        .onFailure { error ->
                            Log.e(TAG, "서버 로그아웃 실패: ${error.message}", error)
                            // 서버 실패해도 로컬 데이터는 삭제하고 로그인 화면으로 이동
                        }

                    _logoutResult.emit(result)
                } else {
                    Log.w(TAG, "RefreshToken이 비어있어 서버 로그아웃 스킵")
                    _logoutResult.emit(Result.success(Unit))
                }

                // 로컬 데이터 삭제
                clearLocalData()

                baseEvent(UiEvent.Loading.Hide)
                baseEvent(UiEvent.Toast.Success("로그아웃되었습니다"))

                // 로그인 화면으로 이동 신호
                _navigateToLogin.emit(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "로그아웃 중 예외 발생:", e)
                baseEvent(UiEvent.Loading.Hide)
                baseEvent(UiEvent.Toast.Normal("로그아웃 중 오류가 발생했습니다"))

                // 예외 발생해도 로컬 데이터 삭제 후 로그인 화면으로 이동
                clearLocalData()
                _navigateToLogin.emit(Unit)
            }
        }
    }

    private fun clearLocalData() {
        Log.d(TAG, "로컬 데이터 삭제 시작")
        prefs.removeString("accessToken")
        prefs.removeString("refreshToken")
        prefs.removeString("loginMethod")
        prefs.removeString("userEmail")
        prefs.removeString("userName")
        prefs.removeString("userRole")
        Log.d(TAG, "로컬 데이터 삭제 완료")
    }

    fun getUserInfo(): Pair<String, String> {
        val userName = prefs.getString("userName", "사용자")
        val userEmail = prefs.getString("userEmail", "user@example.com")
        return Pair(userName, userEmail)
    }
}