package com.yju.presentation.view.speech

import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment


/**
 * TTS 소스 타입 - 각 화면 유형에 따른 소스 구분
 */
enum class TTSSourceType {
    QUIZ_ACTIVITY,
    KEYBOARD_QUIZ,
    MULTIPLE_CHOICE_QUIZ,
    WORD_DETAIL,
    CUSTOM; // 커스텀 소스 사용 시

    companion object {
        /**
         * 클래스로부터 소스 타입 유추
         */
        fun fromClass(clazz: Class<*>): TTSSourceType {
            return when (clazz.simpleName) {
                "QuizActivity" -> QUIZ_ACTIVITY
                "KeyboardQuizFragment" -> KEYBOARD_QUIZ
                "MultipleChoiceQuizFragment" -> MULTIPLE_CHOICE_QUIZ
                "WordDetailFragment" -> WORD_DETAIL
                else -> CUSTOM
            }
        }
    }
}

/**
 * TTS 소스 식별자 - 소스 타입과 인스턴스 해시를 결합한 고유 ID
 */
data class TTSSourceIdentifier(
    val type: TTSSourceType,
    val instanceId: Int = 0
) {
    /**
     * 고유 소스 ID 문자열 생성
     */
    fun asString(): String = "${type.name.lowercase()}_${instanceId}"

    companion object {
        /**
         * Fragment에서 소스 식별자 생성
         */
        fun fromFragment(fragment: Fragment): TTSSourceIdentifier {
            val type = TTSSourceType.fromClass(fragment.javaClass)
            return TTSSourceIdentifier(type, fragment.hashCode())
        }

        /**
         * 객체에서 소스 식별자 생성
         */
        fun fromObject(obj: Any): TTSSourceIdentifier {
            val type = TTSSourceType.fromClass(obj.javaClass)
            return TTSSourceIdentifier(type, obj.hashCode())
        }

        /**
         * 커스텀 소스 식별자 생성
         */
        fun custom(id: String): TTSSourceIdentifier {
            return TTSSourceIdentifier(TTSSourceType.CUSTOM, id.hashCode())
        }
    }
}

/**
 * TTS 시각화 컨텍스트 - 시각화 관련 뷰와 설정을 포함
 */
data class TTSVisualizerContext(
    val container: View,
    val waveView: AudioWaveView,
    val durationText: TextView
)

enum class QuizStep {
    NONE,
    KEYBOARD,
    MULTIPLE_CHOICE,
    WORD_DETAIL,
    EXAMPLE
}
