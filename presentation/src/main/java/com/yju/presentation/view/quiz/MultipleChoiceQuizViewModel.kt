package com.yju.presentation.view.quiz

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
class MultipleChoiceQuizViewModel @Inject constructor(
    savedState: SavedStateHandle
) : BaseViewModel() {

    // 문제 단어 데이터
    private val _word = MutableLiveData<KanjiDetailModel>()
    val word: LiveData<KanjiDetailModel> = _word
    // 선택지 목록 (4지선다)
    private val _options = MutableLiveData<List<String>>()
    val options: LiveData<List<String>> = _options
    // 답변 결과
    private val _answerResult = MutableStateFlow<AnswerResult>(AnswerResult.None)
    val answerResult: StateFlow<AnswerResult> = _answerResult
    // 퀴즈 통과 이벤트
    private val _quizPassed = MutableEventFlow<Boolean>()
    val quizPassed: EventFlow<Boolean> = _quizPassed

    // 정답 인덱스 (0~3)
    private var correctOptionIndex = 0

    // 가짜 선택지 풀 (실제로는 DB에서 가져와야 함)
    private val fakeMeaningPool = listOf(
        "명사 : 마음",
        "동사 : 걷다",
        "형용사 : 빠르다",
        "명사 : 친구",
        "명사 : 시간",
        "동사 : 먹다",
        "동사 : 말하다",
        "형용사 : 작다",
        "명사 : 학교",
        "형용사 : 크다"
    )

    // 답변 결과를 나타내는 sealed class
    sealed class AnswerResult {
        object None : AnswerResult()
        data class Correct(val optionIndex: Int) : AnswerResult()
        data class Incorrect(val optionIndex: Int, val correctOptionIndex: Int) : AnswerResult()
    }

    // 단어 설정 및 선택지 생성
    fun setWord(word: KanjiDetailModel) {
        _word.value = word
        generateOptions(word)
    }

    // 4개의 선택지 생성 (1개 정답, 3개 오답)
    private fun generateOptions(word: KanjiDetailModel) {
        val correctMeaning = word.means
        val incorrectMeanings = getRandomIncorrectMeanings(correctMeaning, 3)

        // 4개의 선택지를 생성하고 섞기
        val allOptions = mutableListOf(correctMeaning).apply {
            addAll(incorrectMeanings)
        }.shuffled()

        // 정답 인덱스 저장
        correctOptionIndex = allOptions.indexOf(correctMeaning)

        // 선택지 목록 업데이트
        _options.value = allOptions
    }

    // 무작위 오답 생성
    private fun getRandomIncorrectMeanings(correctMeaning: String, count: Int): List<String> {
        return fakeMeaningPool
            .filter { it != correctMeaning }
            .shuffled()
            .take(count)
    }

    // 사용자 답변 확인
    fun checkAnswer(selectedIndex: Int) = viewModelScope.launch {
        if (selectedIndex == correctOptionIndex) {
            _answerResult.value = AnswerResult.Correct(selectedIndex)

            // 1초 후에 퀴즈 통과 이벤트 발생
            delay(1000)
            _quizPassed.emit(true)
        } else {
            _answerResult.value = AnswerResult.Incorrect(selectedIndex, correctOptionIndex)
        }
    }

    // 퀴즈 스킵 (개발 테스트용)
    fun skipQuiz() = viewModelScope.launch {
        _quizPassed.emit(true)
    }

}