package com.yju.presentation.view.speech

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * TTS의 오디오 재생을 시각화하는 확장 클래스
 */
class TTSAudioVisualizer(
    private val ttsHelper: TTSHelper
) {
    companion object {
        private const val TAG = "TTSAudioVisualizer"
        private const val CHARS_PER_SECOND = 3.0
        private const val MIN_DURATION_MS = 4800L
        private const val UPDATE_INTERVAL_MS = 100L
    }

    // UI 요소를 WeakReference로 관리하여 메모리 누수 방지
    private var audioWaveContainerRef: WeakReference<View>? = null
    private var audioWaveViewRef: WeakReference<AudioWaveView>? = null
    private var durationTextViewRef: WeakReference<TextView>? = null

    // 현재 재생 상태
    private var isPlaying = false
    private var isRepeating = false
    private var currentText = ""
    private var currentDuration = 0L

    // 타이머 핸들러
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                updateDurationText()
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
    }

    // 반복 재생용 핸들러
    private val repeatRunnable = object : Runnable {
        override fun run() {
            if (isRepeating && isPlaying) {
                Log.d(TAG, "오디오 시각화 반복: $currentText")

                // 애니메이션 초기화 및 재시작
                audioWaveViewRef?.get()?.let {
                    it.stopAnimation()
                    it.startAnimation(currentDuration)
                }

                // 타이머 초기화 및 재시작
                updateDurationText()

                // 다음 반복 예약
                handler.postDelayed(this, TTSHelper.REPEAT_DELAY_MS)
            } else {
                Log.d(TAG, "반복 재생 조건 불충족: isRepeating=$isRepeating, isPlaying=$isPlaying")
                stopVisualization()
            }
        }
    }

    /**
     * UI 요소 연결
     */
    fun bindViews(
        audioWaveContainer: View,
        audioWaveView: AudioWaveView,
        durationTextView: TextView
    ) {
        // 기존 참조 제거
        audioWaveContainerRef?.clear()
        audioWaveViewRef?.clear()
        durationTextViewRef?.clear()

        // 새 WeakReference 생성
        audioWaveContainerRef = WeakReference(audioWaveContainer)
        audioWaveViewRef = WeakReference(audioWaveView)
        durationTextViewRef = WeakReference(durationTextView)

        // 기본 상태 숨김
        audioWaveContainer.visibility = View.GONE
    }

    /**
     * 일회성 재생 시각화 시작
     */
    fun startVisualization(text: String) {
        // 반복 재생 중지
        stopRepeating()
        // 일회성 시각화 시작
        startOnceVisualization(text)
    }

    /**
     * 반복 재생 시각화 시작
     */
    fun startRepeatingVisualization(text: String) {
        // 반복 중지
        stopRepeating()

        // 상태 설정
        currentText = text
        currentDuration = calculateDuration(text)
        isPlaying = true
        isRepeating = true

        // 컨테이너 표시
        audioWaveContainerRef?.get()?.visibility = View.VISIBLE

        // 첫 번째 애니메이션 시작
        startOnceVisualization(text)

        // 반복 예약
        handler.removeCallbacks(repeatRunnable)
        handler.postDelayed(repeatRunnable, TTSHelper.REPEAT_DELAY_MS)
        Log.d(TAG, "반복 재생 시각화 시작: $text, 주기: ${TTSHelper.REPEAT_DELAY_MS}ms")
    }

    /**
     * 한 번만 시각화 시작 (내부용)
     */
    private fun startOnceVisualization(text: String) {
        currentText = text
        currentDuration = calculateDuration(text)
        isPlaying = true

        // 컨테이너 표시
        audioWaveContainerRef?.get()?.visibility = View.VISIBLE

        // 애니메이션 시작
        audioWaveViewRef?.get()?.startAnimation(currentDuration)

        // 시간 업데이트 시작
        updateDurationText()
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS)

        Log.d(TAG, "시각화 시작: $text, 예상 시간: ${currentDuration}ms")
    }

    /**
     * 반복 재생 중지
     */
    private fun stopRepeating() {
        isRepeating = false
        handler.removeCallbacks(repeatRunnable)
        Log.d(TAG, "반복 재생 시각화 중지")
    }

    /**
     * 시각화 중지
     */
    fun stopVisualization() {
        Log.d(TAG, "시각화 중지")

        isPlaying = false
        isRepeating = false

        // 애니메이션 중지
        audioWaveViewRef?.get()?.stopAnimation()

        // 반복 및 업데이트 중지
        handler.removeCallbacks(repeatRunnable)
        handler.removeCallbacks(updateRunnable)

        // 컨테이너 숨김
        audioWaveContainerRef?.get()?.visibility = View.GONE
    }

    /**
     * 시간 표시 업데이트
     */
    private fun updateDurationText() {
        val elapsedTime = audioWaveViewRef?.get()?.getCurrentTime() ?: 0L
        val totalTime = audioWaveViewRef?.get()?.getTotalDuration() ?: 0L

        if (totalTime > 0) {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60

            val timeText = String.format("%02d:%02d", minutes, seconds)
            durationTextViewRef?.get()?.text = timeText
        }
    }

    /**
     * 텍스트 길이에 따른 재생 시간 계산
     */
    private fun calculateDuration(text: String): Long {
        val durationMs = (text.length / CHARS_PER_SECOND * 1000).toLong()
        return maxOf(durationMs, MIN_DURATION_MS)
    }

    /**
     * 리소스 해제
     */
    fun release() {
        Log.d(TAG, "리소스 해제")
        stopVisualization()

        // WeakReference 해제
        audioWaveContainerRef?.clear()
        audioWaveContainerRef = null

        audioWaveViewRef?.clear()
        audioWaveViewRef = null

        durationTextViewRef?.clear()
        durationTextViewRef = null
    }
}