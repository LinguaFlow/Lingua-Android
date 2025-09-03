package com.yju.presentation.view.quiz

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.domain.known.model.TranslationExamplesModel
import com.yju.domain.known.usecase.CreateExampleUseCase
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.MutableEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val createExampleUseCase: CreateExampleUseCase
) : BaseViewModel() {

    private val _onClickBack = MutableEventFlow<Boolean>()
    val onClickBack: EventFlow<Boolean> = _onClickBack

    private val _onClickGenerateExamples = MutableEventFlow<Boolean>()
    val onClickGenerateExamples: EventFlow<Boolean> = _onClickGenerateExamples

    private val _onClickPlayAudio = MutableEventFlow<String>()
    val onClickPlayAudio: EventFlow<String> = _onClickPlayAudio

    // 오디오 중지
    private val _onClickStopAudio = MutableEventFlow<Boolean>()
    val onClickStopAudio: EventFlow<Boolean> = _onClickStopAudio

    // ----- LiveData ----- //
    // 현재 단어
    private val _word = MutableLiveData<KanjiDetailModel>()
    val word: LiveData<KanjiDetailModel> = _word

    // 오디오 재생 상태
    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    // 번역 예제 데이터
    private val _translationExamples = MutableLiveData<TranslationExamplesModel>()
    val translationExamples: LiveData<TranslationExamplesModel> = _translationExamples

    /**
     * 현재 단어 설정
     */
    fun setWord(word: KanjiDetailModel) {
        _word.value = word
    }

    /**
     * 뒤로가기 버튼 클릭
     */
    fun onClickBack() = viewModelScope.launch {
        _onClickBack.emit(true)
    }

    /**
     * 예문 생성 버튼 클릭
     */
    fun onClickGenerateExamples() = viewModelScope.launch {
        _onClickGenerateExamples.emit(true)
        generateExamples()
    }

    /**
     * 예문 생성 처리
     */
    private fun generateExamples() = viewModelScope.launch {
        val word = _word.value?.kanji ?: return@launch
        val jlptLevel = _word.value?.level ?: return@launch
        baseEvent(UiEvent.Loading.Show)
        try {
            createExampleUseCase.getTranslationExamples(word, jlptLevel).fold(
                onSuccess = { examples ->
                    _translationExamples.value = examples
                    baseEvent(UiEvent.Toast.Success("${examples.examples.size}개의 예문이 생성되었습니다"))
                },
                onFailure = { error ->
                    baseEvent(UiEvent.Toast.Normal("예문을 불러오는데 실패했습니다: ${error.message}"))
                }
            )
        } catch (e: Exception) {
            baseEvent(UiEvent.Toast.Normal("네트워크 오류가 발생했습니다"))
        } finally {
            baseEvent(UiEvent.Loading.Hide)
        }
    }

    fun onClickPlayAudio(exampleText: String) = viewModelScope.launch {
        // 재생 중이면 중지
        if (isPlaying.value == true) {
            onClickStopAudio()
            return@launch
        }
        _isPlaying.value = true
        _onClickPlayAudio.emit(exampleText)
    }

    fun onClickStopAudio() = viewModelScope.launch {
        if (isPlaying.value == true) {
            _isPlaying.value = false
            _onClickStopAudio.emit(true)
        }
    }
}