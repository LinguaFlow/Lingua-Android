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

                if (refreshToken.isNotEmpty()) {

                    val result = logoutUseCase(refreshToken)

                    _logoutResult.emit(result)
                } else {
                    Log.w(TAG, "RefreshToken이 비어있어 서버 로그아웃 스킵")
                    _logoutResult.emit(Result.success(Unit))
                }
                clearLocalData()
                baseEvent(UiEvent.Loading.Hide)
                baseEvent(UiEvent.Toast.Success("로그아웃되었습니다"))
                // 로그인 화면으로 이동 신호
                _navigateToLogin.emit(Unit)

            } catch (e: Exception) {
                baseEvent(UiEvent.Loading.Hide)
                baseEvent(UiEvent.Toast.Normal("로그아웃 중 오류가 발생했습니다"))
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