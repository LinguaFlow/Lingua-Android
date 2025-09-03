package com.yju.presentation.view.speech

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TTSHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TTSHelper"
        private val JAPANESE_LOCALE = Locale.JAPANESE
        const val REPEAT_DELAY_MS = 5000L
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isPlaying = false
    private var isRepeating = false
    private var currentText = ""
    private var activeSourceId: String? = null

    // 초기화 작업 중인지 여부
    private var isInitializing = false

    // 초기화 대기 콜백 리스트
    private val pendingInitCallbacks = mutableListOf<(Boolean) -> Unit>()

    // 중복 호출 방지를 위한 상태 동기화 객체
    private val stateLock = Any()

    // 반복 재생을 위한 핸들러
    private val handler = Handler(Looper.getMainLooper())
    private val repeatRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "Repeating TTS: $currentText, sourceId: $activeSourceId")

            synchronized(stateLock) {
                if (isRepeating && isInitialized && activeSourceId != null) {
                    speakOnce(currentText)
                    // 다음 반복을 예약
                    handler.postDelayed(this, REPEAT_DELAY_MS)
                } else {
                    Log.d(TAG, "반복 취소됨: isRepeating=$isRepeating, isInitialized=$isInitialized, activeSourceId=$activeSourceId")
                    // 조건이 맞지 않으면 반복 중지
                    stopInternal()
                }
            }
        }
    }

    /**
     * TTS 초기화
     * 중복 초기화 방지 및 대기 콜백 관리
     */
    fun initialize(onInitialized: (Boolean) -> Unit = {}) {
        synchronized(stateLock) {
            // 이미 초기화된 경우
            if (isInitialized && tts != null) {
                Log.d(TAG, "TTS 이미 초기화됨, 콜백 즉시 호출")
                onInitialized(true)
                return
            }

            // 초기화 작업 중인 경우, 대기 콜백에 추가
            if (isInitializing) {
                Log.d(TAG, "TTS 초기화 작업 중, 콜백 대기 목록에 추가")
                pendingInitCallbacks.add(onInitialized)
                return
            }

            // 초기화 작업 시작
            isInitializing = true
            pendingInitCallbacks.add(onInitialized)
        }

        try {
            // 기존 TTS가 있다면 정리
            release()

            Log.d(TAG, "TTS 초기화 시작")
            tts = TextToSpeech(context) { status ->
                if (status != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS 엔진 초기화 실패: status=$status")

                    synchronized(stateLock) {
                        isInitialized = false
                        isInitializing = false

                        // 모든 대기 콜백에 실패 알림
                        val callbacks = pendingInitCallbacks.toList()
                        pendingInitCallbacks.clear()

                        callbacks.forEach { it(false) }
                    }
                    return@TextToSpeech
                }

                // 일본어 설정 시도
                val result = tts?.setLanguage(JAPANESE_LOCALE)
                val success = when (result) {
                    TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                        Log.w(TAG, "일본어 TTS가 지원되지 않지만 계속 진행합니다")
                        true
                    }
                    else -> {
                        Log.d(TAG, "일본어 TTS 초기화 성공")
                        true
                    }
                }

                setupUtteranceListener()

                synchronized(stateLock) {
                    isInitialized = success
                    isInitializing = false

                    // 모든 대기 콜백에 결과 알림
                    val callbacks = pendingInitCallbacks.toList()
                    pendingInitCallbacks.clear()

                    callbacks.forEach { it(success) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS 초기화 중 예외 발생: ${e.message}")

            synchronized(stateLock) {
                isInitialized = false
                isInitializing = false

                // 모든 대기 콜백에 실패 알림
                val callbacks = pendingInitCallbacks.toList()
                pendingInitCallbacks.clear()

                callbacks.forEach { it(false) }
            }
        }
    }

    /**
     * UtteranceProgressListener 설정
     */
    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                synchronized(stateLock) {
                    isPlaying = true
                }
                Log.d(TAG, "TTS 재생 시작: $utteranceId, sourceId: $activeSourceId")
            }

            override fun onDone(utteranceId: String) {
                synchronized(stateLock) {
                    isPlaying = false
                }
                Log.d(TAG, "TTS 재생 완료: $utteranceId, sourceId: $activeSourceId")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                handleError(utteranceId, -1)
            }

            override fun onError(utteranceId: String, errorCode: Int) {
                handleError(utteranceId, errorCode)
            }

            private fun handleError(utteranceId: String, errorCode: Int) {
                Log.e(TAG, "TTS 오류 발생: utteranceId=$utteranceId, errorCode=$errorCode")

                synchronized(stateLock) {
                    isPlaying = false
                }

                // 에러 시 반복 중지
                stopInternal()
            }
        })
    }

    /**
     * 텍스트 읽기 (한 번만)
     * 중복 호출 방지 및 예외 처리 강화
     */
    fun speak(
        text: String,
        sourceId: String,
        utteranceId: String = UUID.randomUUID().toString()
    ): Boolean {
        synchronized(stateLock) {
            if (!isInitialized || tts == null) {
                Log.e(TAG, "TTS가 초기화되지 않았습니다")
                return false
            }

            // 같은 소스, 같은 텍스트로 이미 재생 중이면 중복 재생 방지
            if (isPlaying && activeSourceId == sourceId && currentText == text) {
                Log.d(TAG, "이미 같은 텍스트 재생 중: sourceId=$sourceId, text=$text")
                return true
            }

            try {
                // 기존 재생 중지
                stopInternal()

                // 상태 설정
                isRepeating = false
                currentText = text
                activeSourceId = sourceId

                return speakOnce(text, utteranceId)
            } catch (e: Exception) {
                Log.e(TAG, "speak 실행 중 예외 발생: ${e.message}")
                stopInternal()
                return false
            }
        }
    }

    /**
     * 반복 재생 시작
     * 중복 호출 방지 및 예외 처리 강화
     */
    fun speakRepeatedly(
        text: String,
        sourceId: String,
        utteranceId: String = UUID.randomUUID().toString()
    ): Boolean {
        synchronized(stateLock) {
            if (!isInitialized || tts == null) {
                Log.e(TAG, "TTS가 초기화되지 않았습니다")
                return false
            }

            // 같은 소스, 같은 텍스트로 이미 반복 재생 중이면 중복 방지
            if (isRepeating && isPlaying && activeSourceId == sourceId && currentText == text) {
                Log.d(TAG, "이미 같은 텍스트 반복 재생 중: sourceId=$sourceId, text=$text")
                return true
            }

            try {
                // 기존 반복 재생 중지
                stopInternal()

                // 새로운 반복 재생 설정
                currentText = text
                isRepeating = true
                activeSourceId = sourceId
                Log.d(TAG, "반복 재생 시작: $text (sourceId: $sourceId)")

                // 최초 발음 재생
                val success = speakOnce(text, utteranceId)

                if (success) {
                    // 다음 반복 예약
                    handler.removeCallbacks(repeatRunnable)
                    handler.postDelayed(repeatRunnable, REPEAT_DELAY_MS)
                    Log.d(TAG, "다음 반복 예약됨: ${REPEAT_DELAY_MS}ms 후")
                } else {
                    // 실패 시 상태 초기화
                    stopInternal()
                }

                return success
            } catch (e: Exception) {
                Log.e(TAG, "speakRepeatedly 실행 중 예외 발생: ${e.message}")
                stopInternal()
                return false
            }
        }
    }

    /**
     * 한 번만 텍스트 읽기 (내부용)
     */
    private fun speakOnce(
        text: String,
        utteranceId: String = UUID.randomUUID().toString()
    ): Boolean {
        if (!isInitialized || tts == null) return false

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        Log.d(TAG, "TTS 재생 요청: $text (result=${result == TextToSpeech.SUCCESS}, sourceId: $activeSourceId)")

        return result == TextToSpeech.SUCCESS
    }

    /**
     * 특정 소스 ID로 시작된 재생 중지
     * 중복 호출 방지 강화
     */
    fun stopForSource(sourceId: String) {
        synchronized(stateLock) {
            if (!isPlaying && !isRepeating) {
                Log.d(TAG, "이미 중지된 상태에서 stopForSource($sourceId) 호출")
                return
            }

            if (activeSourceId == sourceId) {
                Log.d(TAG, "특정 소스($sourceId)에 대한 TTS 재생 중지")
                stopInternal()
            } else {
                Log.d(TAG, "소스 ID 불일치: 현재=$activeSourceId, 요청=$sourceId")
            }
        }
    }

    /**
     * 재생 중지 (일회성 및 반복)
     * 중복 호출 방지 강화
     */
    fun stop() {
        synchronized(stateLock) {
            if (!isPlaying && !isRepeating) {
                Log.d(TAG, "이미 중지된 상태에서 stop() 호출")
                return
            }

            Log.d(TAG, "TTS 재생 중지 요청")
            stopInternal()
        }
    }

    /**
     * 내부 중지 로직 (중복 코드 방지)
     * 동기화 대상에서 제외하여 재귀적 데드락 방지
     */
    private fun stopInternal() {
        Log.d(TAG, "TTS 내부 중지 수행 (activeSourceId: $activeSourceId, isRepeating: $isRepeating)")

        // 반복 중지
        handler.removeCallbacks(repeatRunnable)

        // TTS 중지
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "TTS 중지 중 오류: ${e.message}")
        }

        // 상태 초기화
        isPlaying = false
        isRepeating = false
        currentText = ""
        activeSourceId = null
    }

    /**
     * 리소스 해제
     * 중복 호출 방지 강화
     */
    fun release() {
        synchronized(stateLock) {
            if (!isInitialized && tts == null) {
                Log.d(TAG, "이미 release된 상태에서 release() 호출")
                return
            }

            Log.d(TAG, "TTS 리소스 해제")

            // 먼저 재생 중지
            stopInternal()

            // TTS 엔진 종료
            try {
                tts?.shutdown()
            } catch (e: Exception) {
                Log.e(TAG, "TTS 종료 중 오류: ${e.message}")
            }

            tts = null
            isInitialized = false
            isInitializing = false
            pendingInitCallbacks.clear()
        }
    }

    /**
     * 현재 TTS 재생 상태 확인
     */
    fun isPlaying(): Boolean = synchronized(stateLock) { isPlaying }

    /**
     * 현재 TTS 반복 상태 확인
     */
    fun isRepeating(): Boolean = synchronized(stateLock) { isRepeating }

    /**
     * 활성 소스 ID 반환
     */
    fun getActiveSourceId(): String? = synchronized(stateLock) { activeSourceId }

    /**
     * TTS 초기화 여부 확인
     */
    fun isInitialized(): Boolean = synchronized(stateLock) { isInitialized }
}