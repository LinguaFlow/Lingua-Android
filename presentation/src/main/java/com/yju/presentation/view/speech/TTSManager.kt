package com.yju.presentation.view.speech

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 중앙화된 TTS 관리자
 * 앱 전체에서 단일 인스턴스로 TTS 재생/중지 관리
 */
@Singleton
class TTSManager @Inject constructor(
    private val ttsHelper: TTSHelper
) {
    private val TAG = "TTSManager"

    // 소스 식별자별 시각화 관리자 맵
    private val visualizerMap = mutableMapOf<String, TTSAudioVisualizer>()

    // 현재 활성화된 소스 ID
    private var activeSourceId: String? = null

    // TTS 초기화 상태
    private var isInitializing = false

    // 지연 초기화 플래그
    private var isLazyInitPending = false

    /**
     * Fragment의 라이프사이클에 TTS 관리자 바인딩
     * - 자동으로 Fragment 라이프사이클에 따라 리소스 해제
     */
    fun bindToFragmentLifecycle(fragment: Fragment, visualizerContext: TTSVisualizerContext? = null) {
        val sourceId = TTSSourceIdentifier.fromFragment(fragment).asString()

        // 시각화 컴포넌트가 제공된 경우 등록
        visualizerContext?.let {
            registerVisualizer(sourceId, it.container, it.waveView, it.durationText)
        }

        // 라이프사이클 감시자 추가
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                if (isPlayingSource(sourceId)) {
                    stopPlayback(sourceId)
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                unregisterVisualizer(sourceId)
                fragment.lifecycle.removeObserver(this)
            }
        })
    }

    /**
     * TTS 초기화 - 지연 초기화 지원
     * @param lazy 지연 초기화 사용 여부 (기본값: false)
     */
    fun initialize(lazy: Boolean = false) {
        // 이미 초기화 중이면 중복 호출 방지
        if (isInitializing) {
            Log.d(TAG, "이미 TTS 초기화 작업 중")
            return
        }

        // 지연 초기화 설정
        if (lazy) {
            Log.d(TAG, "TTS 지연 초기화 설정됨 - 실제 사용 시점에 초기화")
            isLazyInitPending = true
            return
        }

        // 직접 초기화
        isInitializing = true
        ttsHelper.initialize { success ->
            isInitializing = false

            if (success) {
                Log.d(TAG, "TTS 초기화 완료")
            } else {
                Log.d(TAG, "TTS 초기화 실패")
            }
        }
    }

    /**
     * 필요시 지연 초기화 실행
     */
    private fun ensureInitialized(callback: (Boolean) -> Unit) {
        if (ttsHelper.isInitialized()) {
            // 이미 초기화됨
            callback(true)
            return
        }

        if (isInitializing) {
            // 이미 초기화 중
            Log.d(TAG, "TTS 초기화 중... 잠시 후 다시 시도")
            callback(false)
            return
        }

        // 지연 초기화 실행
        isInitializing = true
        Log.d(TAG, "TTS 지연 초기화 실행")

        ttsHelper.initialize { success ->
            isInitializing = false
            isLazyInitPending = false

            if (success) {
                Log.d(TAG, "TTS 지연 초기화 완료")
            } else {
                Log.d(TAG, "TTS 지연 초기화 실패")
            }

            callback(success)
        }
    }

    /**
     * 시각화 컴포넌트 등록
     */
    fun registerVisualizer(
        sourceId: String,
        container: View,
        waveView: AudioWaveView,
        durationText: TextView
    ) {
        // 기존 시각화 객체가 있으면 해제
        unregisterVisualizer(sourceId)

        // 새 시각화 객체 생성 및 등록
        val visualizer = TTSAudioVisualizer(ttsHelper).apply {
            bindViews(container, waveView, durationText)
        }

        visualizerMap[sourceId] = visualizer
        Log.d(TAG, "시각화 컴포넌트 등록: sourceId=$sourceId")
    }

    /**
     * 시각화 컴포넌트 해제
     */
    fun unregisterVisualizer(sourceId: String) {
        visualizerMap[sourceId]?.let {
            it.release()
            visualizerMap.remove(sourceId)
            Log.d(TAG, "시각화 컴포넌트 해제: sourceId=$sourceId")
        }
    }

    /**
     * 텍스트 재생 (반복 없음)
     * 지연 초기화 지원
     */
    fun playText(
        text: String,
        source: Any
    ): Boolean {
        val sourceId = TTSSourceIdentifier.fromObject(source).asString()
        return playText(text, sourceId)
    }

    /**
     * 텍스트 재생 (반복 없음, 소스 ID 직접 지정)
     * 지연 초기화 지원
     */
    fun playText(
        text: String,
        sourceId: String
    ): Boolean {
        // 재생 중인 다른 소스가 있으면 중지
        if (activeSourceId != null && activeSourceId != sourceId) {
            stopPlayback(activeSourceId!!)
        }

        // TTS가 초기화되지 않았고 지연 초기화가 설정되었으면 초기화 후 재생
        if (!ttsHelper.isInitialized() && (isLazyInitPending || !isInitializing)) {
            Log.d(TAG, "TTS 지연 초기화 후 재생 시도: $sourceId")
            ensureInitialized { success ->
                if (success) {
                    playTextInternal(text, sourceId, false)
                }
            }
            return true // 지연 초기화 중임을 알림
        }

        // 일반 재생
        return playTextInternal(text, sourceId, false)
    }

    /**
     * 텍스트 반복 재생
     * 지연 초기화 지원
     */
    fun playTextRepeatedly(
        text: String,
        source: Any
    ): Boolean {
        val sourceId = TTSSourceIdentifier.fromObject(source).asString()
        return playTextRepeatedly(text, sourceId)
    }

    /**
     * 텍스트 반복 재생 (소스 ID 직접 지정)
     * 지연 초기화 지원
     */
    fun playTextRepeatedly(
        text: String,
        sourceId: String
    ): Boolean {
        // 재생 중인 다른 소스가 있으면 중지
        if (activeSourceId != null && activeSourceId != sourceId) {
            stopPlayback(activeSourceId!!)
        }

        // TTS가 초기화되지 않았고 지연 초기화가 설정되었으면 초기화 후 재생
        if (!ttsHelper.isInitialized() && (isLazyInitPending || !isInitializing)) {
            Log.d(TAG, "TTS 지연 초기화 후 반복 재생 시도: $sourceId")
            ensureInitialized { success ->
                if (success) {
                    playTextInternal(text, sourceId, true)
                }
            }
            return true // 지연 초기화 중임을 알림
        }

        // 일반 반복 재생
        return playTextInternal(text, sourceId, true)
    }

    /**
     * 텍스트 재생 내부 메서드
     */
    private fun playTextInternal(text: String, sourceId: String, repeat: Boolean): Boolean {
        if (!ttsHelper.isInitialized()) {
            Log.d(TAG, "TTS가 초기화되지 않아 재생할 수 없음: $sourceId")
            return false
        }

        // 재생 시작
        val success = if (repeat) {
            ttsHelper.speakRepeatedly(text, sourceId)
        } else {
            ttsHelper.speak(text, sourceId)
        }

        if (success) {
            activeSourceId = sourceId

            // 시각화 시작
            visualizerMap[sourceId]?.let {
                if (repeat) {
                    it.startRepeatingVisualization(text)
                } else {
                    it.startVisualization(text)
                }
            }

            Log.d(TAG, "텍스트 ${if (repeat) "반복 " else ""}재생 시작: sourceId=$sourceId, text=$text")
        } else {
            Log.d(TAG, "텍스트 재생 실패: sourceId=$sourceId")
        }

        return success
    }

    /**
     * 특정 소스의 재생 중지 (객체 기반)
     */
    fun stopPlayback(source: Any) {
        val sourceId = TTSSourceIdentifier.fromObject(source).asString()
        stopPlayback(sourceId)
    }

    /**
     * 특정 소스의 재생 중지 (소스 ID 기반)
     */
    fun stopPlayback(sourceId: String) {
        if (activeSourceId == sourceId) {
            ttsHelper.stopForSource(sourceId)
            visualizerMap[sourceId]?.stopVisualization()
            activeSourceId = null

            Log.d(TAG, "재생 중지: sourceId=$sourceId")
        }
    }

    /**
     * 모든 재생 중지
     */
    fun stopAllPlayback() {
        activeSourceId?.let { sourceId ->
            ttsHelper.stopForSource(sourceId)
            visualizerMap[sourceId]?.stopVisualization()
            activeSourceId = null

            Log.d(TAG, "모든 재생 중지: 마지막 활성 sourceId=$sourceId")
        }
    }

    /**
     * 리소스 해제
     * 안전 메커니즘 강화
     */
    fun release() {
        stopAllPlayback()

        // 모든 시각화 컴포넌트 해제
        visualizerMap.forEach { (sourceId, visualizer) ->
            visualizer.release()
            Log.d(TAG, "시각화 컴포넌트 해제: sourceId=$sourceId")
        }
        visualizerMap.clear()

        // TTS 헬퍼가 초기화된 경우에만 해제 (중복 해제 방지)
        if (ttsHelper.isInitialized()) {
            ttsHelper.release()
            Log.d(TAG, "TTSManager 리소스 해제 완료")
        } else {
            Log.d(TAG, "TTS가 초기화되지 않아 release() 생략")
        }

        // 상태 초기화
        isLazyInitPending = false
        isInitializing = false
    }

    /**
     * 재생 중인지 확인
     */
    fun isPlaying(): Boolean {
        return ttsHelper.isPlaying()
    }

    /**
     * 특정 소스가 재생 중인지 확인 (객체 기반)
     */
    fun isPlayingSource(source: Any): Boolean {
        val sourceId = TTSSourceIdentifier.fromObject(source).asString()
        return isPlayingSource(sourceId)
    }

    /**
     * 특정 소스가 재생 중인지 확인 (소스 ID 기반)
     */
    fun isPlayingSource(sourceId: String): Boolean {
        return ttsHelper.isPlaying() && ttsHelper.getActiveSourceId() == sourceId
    }

    /**
     * TTS 초기화 여부 확인
     */
    fun isInitialized(): Boolean {
        return ttsHelper.isInitialized()
    }
}