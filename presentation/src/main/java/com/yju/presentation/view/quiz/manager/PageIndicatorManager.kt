package com.yju.presentation.view.quiz.manager

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.yju.presentation.R

/**
 * 페이지 인디케이터 매니저
 * - 위치 동기화 문제 해결 및 최대 10개 제한
 */
class PageIndicatorManager(
    private val context: Context,
    private val containerView: LinearLayout
) {
    companion object {
        private const val TAG = "PageIndicatorManager"
        private const val MAX_DOTS = 10  // 최대 도트 개수 제한
    }

    // 인디케이터 저장용 리스트
    private val dots = ArrayList<ImageView>()

    // 현재 선택된 위치
    private var currentPosition = -1

    init {
        // 컨테이너 초기 설정
        containerView.apply {
            removeAllViews()  // 기존 뷰 제거
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        Log.d(TAG, "PageIndicatorManager 초기화 완료")
    }

    /**
     * 인디케이터 생성
     * @param count 페이지 개수
     */
    fun createIndicators(count: Int) {
        // 기존 도트 제거
        dots.clear()
        containerView.removeAllViews()
        currentPosition = -1

        // 1개 이하면 표시 안함
        if (count <= 1) {
            containerView.visibility = View.GONE
            Log.d(TAG, "인디케이터 생성 취소: 페이지 수 $count")
            return
        }

        // 최대 10개로 제한
        val actualCount = Math.min(count, MAX_DOTS)
        Log.d(TAG, "인디케이터 생성: 요청=$count, 실제=$actualCount")

        try {
            // 아이템 크기 계산
            val size = context.resources.getDimensionPixelSize(R.dimen.indicator_size)
            val margin = context.resources.getDimensionPixelSize(R.dimen.indicator_margin)

            // 인디케이터 생성 및 추가
            for (i in 0 until actualCount) {
                val dot = createDot(i, size, margin)
                containerView.addView(dot)
                dots.add(dot)
            }

            // 컨테이너 표시
            containerView.visibility = View.VISIBLE

            Log.d(TAG, "인디케이터 생성 완료: ${dots.size}개")
        } catch (e: Exception) {
            Log.e(TAG, "인디케이터 생성 실패: ${e.message}", e)
        }
    }

    /**
     * 단일 인디케이터 도트 생성
     */
    private fun createDot(index: Int, size: Int, margin: Int): ImageView {
        return ImageView(context).apply {
            // 레이아웃 파라미터
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, 0, margin, 0)
            }

            // 도트 이미지 설정
            setImageResource(R.drawable.indicator_dot_selector)

            // 기본은 비선택 상태
            isSelected = false
        }
    }

    /**
     * 인디케이터 업데이트
     * @param position 현재 선택된 위치
     * @param totalCount 전체 페이지 수 (사용하지 않지만 호환성 유지)
     */
    fun updateIndicators(position: Int, totalCount: Int) {
        // 기본 유효성 검사
        if (dots.isEmpty()) {
            Log.d(TAG, "인디케이터 없음, 업데이트 무시")
            return
        }

        // 중복 업데이트 방지
        if (position == currentPosition) {
            Log.d(TAG, "인디케이터 중복 업데이트 무시: $position")
            return
        }

        try {
            // 위치 정보 업데이트
            currentPosition = position

            // 실제 도트 개수 내에서만 처리 (안전장치)
            val visualPosition = position.coerceIn(0, dots.size - 1)

            // 각 도트 상태 업데이트
            for (i in dots.indices) {
                val shouldBeSelected = (i == visualPosition)
                if (dots[i].isSelected != shouldBeSelected) {
                    dots[i].isSelected = shouldBeSelected
                }
            }

            Log.d(TAG, "인디케이터 업데이트: 위치=$position, 표시=$visualPosition (총 ${dots.size}개)")
        } catch (e: Exception) {
            Log.e(TAG, "인디케이터 업데이트 실패: ${e.message}", e)
        }
    }

    /**
     * 모든 리소스 정리
     */
    fun clear() {
        try {
            // 모든 도트 제거
            dots.clear()
            containerView.removeAllViews()
            containerView.visibility = View.GONE
            currentPosition = -1

            Log.d(TAG, "인디케이터 정리 완료")
        } catch (e: Exception) {
            Log.e(TAG, "인디케이터 정리 실패: ${e.message}", e)
        }
    }

    /**
     * 특정 위치의 인디케이터 강제 선택
     * - 현재 위치 동기화 문제 해결용
     */
    fun forceSelectPosition(position: Int) {
        if (dots.isEmpty()) return

        val safePosition = position.coerceIn(0, dots.size - 1)
        Log.d(TAG, "인디케이터 위치 강제 선택: $safePosition / ${dots.size}")

        updateIndicators(safePosition, dots.size)
    }
}