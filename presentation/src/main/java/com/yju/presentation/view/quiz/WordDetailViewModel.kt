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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    // 단어 정보
    private val _word = MutableLiveData<KanjiDetailModel>()
    val word: LiveData<KanjiDetailModel> = _word

    // 뒤로가기 이벤트
    private val _back = MutableEventFlow<Boolean>()
    val back: EventFlow<Boolean> = _back

    // 순서+품사를 표시할 문자열
    private val _typeLabel = MutableLiveData<String>()
    val typeLabel: LiveData<String> = _typeLabel

    // 발음 재생 이벤트
    private val _playAudioEvent = MutableEventFlow<String>()
    val playAudioEvent: EventFlow<String> = _playAudioEvent

    // 발음 중지 이벤트
    private val _stopAudioEvent = MutableEventFlow<Boolean>()
    val stopAudioEvent: EventFlow<Boolean> = _stopAudioEvent

    // 오디오 재생 상태
    private val _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    // 뒤로가기 버튼 클릭
    fun onClickBack() = viewModelScope.launch {
        _back.emit(true)
    }

    // 단어 설정
    fun setWord(word: KanjiDetailModel) {
        _word.value = word
        // 순서+품사 설정
        val part = word.means.substringBefore(":", "")
        _typeLabel.value = "${word.vocabularyBookOrder}${if (part.isNotBlank()) " $part" else ""}"
    }

    // 발음 재생/중지 토글
    fun toggleAudio() = viewModelScope.launch {
        val currentWord = _word.value ?: return@launch
        val furigana = currentWord.furigana
        val isCurrentlyPlaying = isPlaying.value ?: false
        if (!isCurrentlyPlaying) {
            _isPlaying.value = true
            _playAudioEvent.emit(furigana)
        }
    }

    // 발음 재생 중지
    fun stopAudio() = viewModelScope.launch {
        val wasPlaying = _isPlaying.value ?: false

        // 상태 먼저 변경
        _isPlaying.value = false
        _stopAudioEvent.emit(true)

        // 토스트 메시지는 한 번만 표시
        if (wasPlaying) {
            baseEvent(UiEvent.Toast.Normal("발음 재생을 중지합니다"))
        }
    }
}