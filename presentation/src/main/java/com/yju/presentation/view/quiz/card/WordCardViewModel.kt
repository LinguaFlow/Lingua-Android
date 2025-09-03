package com.yju.presentation.view.quiz.card

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.util.EventFlow
import com.yju.presentation.util.MutableEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 단어 카드 뷰모델
 * - 단어 카드의 표시 데이터 관리
 * - 예문 보기 등 사용자 이벤트 처리
 * - 오디오 재생 관련 상태 관리
 */
@HiltViewModel
class WordCardViewModel @Inject constructor() : BaseViewModel() {

    companion object {
        private const val TAG = "WordCardViewModel"
    }

    // 한자 세부 정보
    private val _kanjiDetailFlow = MutableStateFlow<KanjiDetailModel?>(null)
    val kanjiDetailFlow: StateFlow<KanjiDetailModel?> = _kanjiDetailFlow.asStateFlow()

    // 좌우 스와이프 UI 관련 상태
    private val _position = MutableLiveData(0)
    val position: LiveData<Int> = _position

    private val _totalCount = MutableLiveData(0)
    val totalCount: LiveData<Int> = _totalCount

    private val _canSwipe = MutableLiveData(true)
    val canSwipe: LiveData<Boolean> = _canSwipe

    // 예문 보기 이벤트
    private val _showExample = MutableEventFlow<Unit>()
    val showExample: EventFlow<Unit> = _showExample

    // 오디오 관련 상태
    private val _isAudioControlVisible = MutableLiveData(false)
    val isAudioControlVisible: LiveData<Boolean> = _isAudioControlVisible

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    // 오디오 재생 완료 이벤트
    private val _audioPlayComplete = MutableEventFlow<Unit>()
    val audioPlayComplete: EventFlow<Unit> = _audioPlayComplete

    /**
     * 한자 세부 정보 설정
     * @param kanjiDetail 한자 세부 정보
     */
    fun setKanjiDetail(kanjiDetail: KanjiDetailModel) {
        Log.d(TAG, "setKanjiDetail: ${kanjiDetail.kanji}")
        _kanjiDetailFlow.value = kanjiDetail
    }

    /**
     * 위치 정보 설정
     * @param position 현재 위치
     * @param total 전체 개수
     */
    fun setPositionInfo(position: Int, total: Int) {
        _position.value = position
        _totalCount.value = total
        Log.d(TAG, "setPositionInfo: 위치 $position, 전체 $total")
    }

    /**
     * 스와이프 가능 여부 설정
     * @param canSwipe 스와이프 가능 여부
     */
    fun setSwipeEnabled(canSwipe: Boolean) {
        _canSwipe.value = canSwipe
    }

    /**
     * 예문 보기 버튼 클릭 이벤트 처리
     * - 예문 보기 화면으로 이동 이벤트 발행
     */
    fun onShowExample() = viewModelScope.launch {
        Log.d(TAG, "onShowExample: 예문 보기 이벤트 발행")
        _showExample.emit(Unit)
    }

    /**
     * 오디오 재생 버튼 클릭 이벤트 처리
     * - 오디오 컨트롤 가시성 토글
     * - 오디오 재생/정지 처리
     */
    fun onPlayAudio() = viewModelScope.launch {
        Log.d(TAG, "onPlayAudio: 오디오 재생 버튼 클릭")

        // 오디오 컨트롤이 보이지 않을 경우 보이게 함
        if (!_isAudioControlVisible.value!!) {
            _isAudioControlVisible.value = true
        }

        // 오디오 재생 상태 토글
        val newPlayingState = !(_isPlaying.value ?: false)
        _isPlaying.value = newPlayingState

        if (newPlayingState) {
            // 실제 오디오 재생 로직 구현 (별도의 오디오 매니저 서비스 호출 등)
            startAudioPlayback()
        } else {
            // 오디오 정지 로직 구현
            stopAudioPlayback()
        }
    }

    /**
     * 오디오 컨트롤 가시성 설정
     * @param isVisible 가시성 여부
     */
    fun setAudioControlVisible(isVisible: Boolean) {
        Log.d(TAG, "setAudioControlVisible: $isVisible")
        _isAudioControlVisible.value = isVisible

        // 오디오 컨트롤이 숨겨지면 재생도 중지
        if (!isVisible && _isPlaying.value == true) {
            _isPlaying.value = false
            stopAudioPlayback()
        }
    }

    /**
     * 오디오 재생 시작
     * 실제 오디오 재생 구현은 별도 서비스나 매니저를 통해 처리
     */
    private fun startAudioPlayback() {
        Log.d(TAG, "startAudioPlayback: 오디오 재생 시작")
        viewModelScope.launch {
            _isPlaying.value = false
            _audioPlayComplete.emit(Unit)
        }
    }

    /**
     * 오디오 재생 중지
     */
    private fun stopAudioPlayback() {
        Log.d(TAG, "stopAudioPlayback: 오디오 재생 중지")
    }

    /**
     * 화면 이탈 시 정리 작업
     */
    override fun onCleared() {
        super.onCleared()
        // 오디오 리소스 정리
        if (_isPlaying.value == true) {
            stopAudioPlayback()
        }
    }
}