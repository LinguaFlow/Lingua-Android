package com.yju.presentation.view.quiz.sheet

import androidx.lifecycle.viewModelScope
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.MutableEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 퀴즈 메뉴 바텀시트 ViewModel
 * - 바텀시트 상태 및 이벤트 관리
 */
@HiltViewModel
class QuizMenuBottomSheetViewModel @Inject constructor() : BaseViewModel() {

    // 바텀시트 닫기 이벤트
    private val _dismiss = MutableEventFlow<Unit>()
    val dismiss: EventFlow<Unit> = _dismiss

    // 키보드 퀴즈 선택 이벤트
    private val _keyboardQuizSelected = MutableEventFlow<Unit>()
    val keyboardQuizSelected: EventFlow<Unit> = _keyboardQuizSelected

    // 객관식 퀴즈 선택 이벤트
    private val _multipleChoiceQuizSelected = MutableEventFlow<Unit>()
    val multipleChoiceQuizSelected: EventFlow<Unit> = _multipleChoiceQuizSelected

    // 단어 상세 선택 이벤트
    private val _wordDetailSelected = MutableEventFlow<Unit>()
    val wordDetailSelected: EventFlow<Unit> = _wordDetailSelected

    // 예문 선택 이벤트
    private val _exampleSelected = MutableEventFlow<Unit>()
    val exampleSelected: EventFlow<Unit> = _exampleSelected

    /**
     * 바텀시트 닫기
     */
    fun onDismiss() = viewModelScope.launch {
        _dismiss.emit(Unit)
    }

    /**
     * 키보드 퀴즈 선택
     */
    fun onKeyboardQuizSelected() = viewModelScope.launch {
        _keyboardQuizSelected.emit(Unit)
    }

    /**
     * 객관식 퀴즈 선택
     */
    fun onMultipleChoiceQuizSelected() = viewModelScope.launch {
        _multipleChoiceQuizSelected.emit(Unit)
    }

    /**
     * 단어 상세 선택
     */
    fun onWordDetailSelected() = viewModelScope.launch {
        _wordDetailSelected.emit(Unit)
    }

    /**
     * 예문 선택
     */
    fun onExampleSelected() = viewModelScope.launch {
        _exampleSelected.emit(Unit)
    }
}