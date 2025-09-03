package com.yju.presentation.view.speech

import android.app.Activity
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Fragment에 TTS 매니저 바인딩 (간소화된 인터페이스)
 */
fun TTSManager.bindToFragment(
    fragment: Fragment,
    container: View,
    waveView: AudioWaveView,
    durationText: TextView
) {
    // 프래그먼트 라이프사이클에 TTS 관리자 바인딩
    bindToFragmentLifecycle(fragment, TTSVisualizerContext(
        container = container,
        waveView = waveView,
        durationText = durationText
    ))
}

/**
 * Activity에 TTS 매니저 바인딩 (간소화된 인터페이스)
 */
fun TTSManager.bindToActivity(
    activity: Activity,
    container: View,
    waveView: AudioWaveView,
    durationText: TextView
) {
    // 소스 ID 생성
    val sourceId = TTSSourceIdentifier.fromObject(activity).asString()

    // 시각화 컴포넌트 등록
    registerVisualizer(
        sourceId = sourceId,
        container = container,
        waveView = waveView,
        durationText = durationText
    )
}