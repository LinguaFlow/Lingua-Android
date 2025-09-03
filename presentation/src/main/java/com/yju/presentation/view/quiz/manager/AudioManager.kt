package com.yju.presentation.view.quiz.manager

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.yju.presentation.view.speech.AudioWaveView
import com.yju.presentation.view.speech.TTSManager
import com.yju.presentation.view.speech.bindToActivity

/**
 * 오디오 관리 클래스
 * - TTS 관리 및 오디오 재생 처리
 * - 오디오 시각화 UI 연동
 */
class AudioManager(
    private val ttsManager: TTSManager,
    private val activity: FragmentActivity
) {
    companion object {
        private const val TAG = "AudioManager"
    }

    private var isPlaying = false
    private var container: View? = null
    private var waveView: AudioWaveView? = null
    private var durationText: TextView? = null

    /**
     * TTS 초기화
     */
    fun initialize(lazy: Boolean = true) {
        ttsManager.initialize(lazy)
        Log.d(TAG, "TTS 초기화 완료: 지연 초기화=$lazy")
    }

    /**
     * 오디오 시각화 컴포넌트 설정
     */
    fun setupVisualizer(
        audioContainer: View?,
        audioWaveView: View?,
        audioDurationText: TextView?
    ) {
        this.container = audioContainer

        // AudioWaveView 타입 확인
        this.waveView = if (audioWaveView is AudioWaveView) {
            audioWaveView
        } else {
            Log.w(TAG, "오디오 웨이브 뷰 타입 불일치")
            null
        }

        this.durationText = audioDurationText

        if (container != null && waveView != null && durationText != null) {
            ttsManager.bindToActivity(
                activity = activity,
                container = container!!,
                waveView = waveView!!,
                durationText = durationText!!
            )
            Log.d(TAG, "오디오 시각화 설정 완료")
        } else {
            Log.w(TAG, "오디오 시각화 설정 실패: 일부 컴포넌트가 null")
        }
    }

    /**
     * 텍스트 음성 재생
     * @param text 재생할 텍스트
     * @return 재생 성공 여부
     */
    fun playText(text: String): Boolean {
        // 재생 중인 오디오가 있다면 정지
        stopPlayback()

        if (text.isEmpty()) {
            Log.w(TAG, "재생할 텍스트가 비어있음")
            return false
        }

        val success = ttsManager.playTextRepeatedly(text, activity)
        if (success) {
            isPlaying = true
            container?.visibility = View.VISIBLE
            Log.d(TAG, "오디오 재생 시작: \"$text\"")
        } else {
            Log.e(TAG, "오디오 재생 실패")
        }

        return success
    }

    /**
     * 모든 오디오 재생 중지
     */
    fun stopPlayback() {
        ttsManager.stopAllPlayback()
        container?.visibility = View.GONE
        isPlaying = false
        Log.d(TAG, "오디오 재생 중지")
    }

    /**
     * 현재 재생 중인지 확인
     */
    fun isCurrentlyPlaying() = isPlaying
}