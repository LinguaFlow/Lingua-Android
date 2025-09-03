package com.yju.presentation.view.quiz

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.MutableEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyboardQuizViewModel @Inject constructor(
    savedState: SavedStateHandle
) : BaseViewModel() {

    // 문제 단어 데이터
    private val _word = MutableLiveData<KanjiDetailModel>()
    val word: LiveData<KanjiDetailModel> = _word
    // 사용자가 입력한 답변
    private val _userInput = MutableLiveData("")
    val userInput: LiveData<String> = _userInput
    // 답변 상태 (정답/오답/대기중)
    private val _answerState = MutableStateFlow<AnswerState>(AnswerState.Idle)
    val answerState: StateFlow<AnswerState> = _answerState
    // 힌트 표시 이벤트
    private val _showHintEvent = MutableEventFlow<String>()
    val showHintEvent: EventFlow<String> = _showHintEvent
    // 퀴즈 통과 이벤트
    private val _quizPassed = MutableEventFlow<Boolean>()
    val quizPassed: EventFlow<Boolean> = _quizPassed
    // 정답 확인 시도 횟수
    private var attemptCount = 0
    // 정답 처리 중 여부 (중복 처리 방지)
    private var isProcessingAnswer = false


    // 답변 상태를 나타내는 sealed class
    sealed class AnswerState {
        object Idle : AnswerState()
        object Correct : AnswerState()
        object Incorrect : AnswerState()

        override fun toString(): String {
            return when (this) {
                is Idle -> "Idle"
                is Correct -> "Correct"
                is Incorrect -> "Incorrect"
            }
        }
    }

    // 단어 설정
    fun setWord(word: KanjiDetailModel) {
        _word.value = word
        resetState()
    }

    // 사용자 입력 업데이트
    fun updateUserInput(input: String) {
        _userInput.value = input
        // 입력이 변경되면 상태를 Idle로 재설정
        if (_answerState.value != AnswerState.Idle) {
            _answerState.value = AnswerState.Idle
        }
    }

    // 정답 확인
    fun checkAnswer() = viewModelScope.launch {
        try {
            // 중복 처리 방지
            if (isProcessingAnswer) {
                return@launch
            }
            val currentWord = _word.value ?: run {
                return@launch
            }
            val input = _userInput.value ?: ""
            // 처리 시작
            isProcessingAnswer = true
            // 공백 제거 및 대소문자 구분 없이 비교
            val userAnswer = input.trim().lowercase()
            val correctAnswer = currentWord.furigana.trim().lowercase()
            if (userAnswer == correctAnswer) {
                _answerState.value = AnswerState.Correct
                // 1초 후에 퀴즈 통과 이벤트 발생
                delay(1000)
                _quizPassed.emit(true)
            } else {
                _answerState.value = AnswerState.Incorrect
                attemptCount++
            }
        } finally {
            // 처리 완료
            isProcessingAnswer = false
        }
    }

    // 힌트 표시
    fun showHint() = viewModelScope.launch {
        val currentWord = _word.value ?: run {
            return@launch
        }
        // 첫 글자 힌트
        val hint = when {
            attemptCount < 2 -> "첫 글자는 '${currentWord.furigana.firstOrNull() ?: ""}'"
            else -> currentWord.furigana // 두 번 이상 시도했으면 정답 공개
        }
        _showHintEvent.emit(hint)
    }

    // 퀴즈 스킵 (개발 테스트용)
    fun skipQuiz() = viewModelScope.launch {
        _quizPassed.emit(true)
    }

    // 상태 초기화
    private fun resetState() {
        _userInput.value = ""
        _answerState.value = AnswerState.Idle
        attemptCount = 0
        isProcessingAnswer = false

    }

    override fun onCleared() {
        super.onCleared()
    }
}