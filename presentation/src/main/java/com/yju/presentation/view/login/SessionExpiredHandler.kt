package com.yju.presentation.view.login

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionExpiredHandler @Inject constructor() {

    private val _sessionExpiredEvent = MutableSharedFlow<Unit>()
    val sessionExpiredEvent = _sessionExpiredEvent.asSharedFlow()

    /**
     * 세션 만료 이벤트 발행
     */
    suspend fun notifySessionExpired() {
        _sessionExpiredEvent.emit(Unit)
    }

    /**
     * 로그인 화면으로 이동
     */
    fun navigateToLogin(context: Context) {
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("session_expired", true)
        }
        context.startActivity(intent)
    }
}