package com.yju.presentation.view.quiz

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.MutableEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class QuizViewModel @Inject constructor() : BaseViewModel() {
    private var isSettingWord = false
    private var lastWordId = ""
    private val _word = MutableLiveData<KanjiDetailModel>()
    val word: LiveData<KanjiDetailModel> = _word

    private val _words = MutableLiveData<List<KanjiDetailModel>>(emptyList())
    val words: LiveData<List<KanjiDetailModel>> = _words

    private val _currentPosition = MutableLiveData(0)
    val currentPosition: LiveData<Int> = _currentPosition

    private val _hasPrevWord = MutableLiveData<Boolean>(false)
    val hasPrevWord: LiveData<Boolean> = _hasPrevWord

    private val _hasNextWord = MutableLiveData<Boolean>(false)
    val hasNextWord: LiveData<Boolean> = _hasNextWord

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _back = MutableEventFlow<Unit>()
    val back: EventFlow<Unit> = _back

    private val _showMenu = MutableEventFlow<Unit>()
    val showMenu: EventFlow<Unit> = _showMenu

    private val _playAudio = MutableEventFlow<String>()
    val playAudio: EventFlow<String> = _playAudio

    private val _stopAudio = MutableEventFlow<Unit>()
    val stopAudio: EventFlow<Unit> = _stopAudio

    private val _keyboardQuizPassed = MutableEventFlow<Unit>()
    val keyboardQuizPassed: EventFlow<Unit> = _keyboardQuizPassed

    private val _multipleChoiceQuizPassed = MutableEventFlow<Unit>()
    val multipleChoiceQuizPassed: EventFlow<Unit> = _multipleChoiceQuizPassed

    fun onClickBack() = viewModelScope.launch {
        _back.emit(Unit)
    }

    fun onClickMenu() = viewModelScope.launch {
        _showMenu.emit(Unit)
    }

    fun onPlayAudio() {
        val word = _word.value ?: return
        val furigana = word.furigana
        if (isPlaying.value == true) {
            stopAudio()
        } else {
            viewModelScope.launch {
                Log.d("QuizViewModel", "오디오 재생: $furigana")
                _isPlaying.value = true
                _playAudio.emit(furigana)
            }
        }
    }

    fun setWords(words: List<KanjiDetailModel>) {
        Log.d("QuizViewModel", "단어 목록 설정: ${words.size}개")
        _words.value = words
        if (words.isNotEmpty() && _word.value == null) {
            setWord(words[0])
        }
    }

    fun setWord(word: KanjiDetailModel) {
        val wordId = "${word.kanji}_${word.vocabularyBookOrder}"
        val currentWordId = _word.value?.let { "${it.kanji}_${it.vocabularyBookOrder}" } ?: ""

        if (isSettingWord || currentWordId == wordId) {
            return
        }

        try {
            isSettingWord = true
            Log.d("QuizViewModel", "단어 설정: ${word.kanji} (ID: ${word.vocabularyBookOrder})")
            _word.value = word
            lastWordId = wordId
            updatePositionForWord(word)

        } finally {
            isSettingWord = false
        }
    }

    private fun updatePositionForWord(word: KanjiDetailModel) {

        val wordsList = _words.value ?: emptyList()
        val position = wordsList.indexOfFirst {
            it.kanji == word.kanji && it.vocabularyBookOrder == word.vocabularyBookOrder
        }
        if (position != -1 && _currentPosition.value != position) {
            _currentPosition.value = position
            _hasPrevWord.value = position > 0
            _hasNextWord.value = position < wordsList.size - 1
            Log.d(
                "QuizViewModel",
                "위치 업데이트: $position (이전: ${_hasPrevWord.value}, 다음: ${_hasNextWord.value})"
            )
        }
    }

    fun setCurrentPosition(position: Int) {
        val wordsList = _words.value ?: return
        if (position in wordsList.indices) {
            Log.d("QuizViewModel", "현재 위치 설정: $position")
            _currentPosition.value = position
            _word.value = wordsList[position]
            _hasPrevWord.value = position > 0
            _hasNextWord.value = position < wordsList.size - 1
        }
    }

    fun stopAudio() = viewModelScope.launch {
        if (isPlaying.value == true) {
            Log.d("QuizViewModel", "오디오 재생 중지")
            _isPlaying.value = false
            _stopAudio.emit(Unit)
        }
    }

    fun onKeyboardQuizPassed() = viewModelScope.launch {
        _keyboardQuizPassed.emit(Unit)
    }

    fun getWordsCount(): Int {
        return _words.value?.size ?: 0
    }
}