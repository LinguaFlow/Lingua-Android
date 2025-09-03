package com.yju.presentation.view.quiz.manager

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.presentation.view.pdf.adapter.WordCardAdapter
import java.lang.reflect.Field

/**
 * ViewPager2 관리 클래스
 * - 위치 동기화 문제 해결
 */
class ViewPager2Manager(
    private val activity: FragmentActivity,
    private val viewPager: ViewPager2
) {
    companion object {
        private const val TAG = "ViewPager2Manager"
        private var cachedRecyclerViewField: Field? = null
    }

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private lateinit var wordCardAdapter: WordCardAdapter
    private var currentPosition = 0

    // 위치 변경 콜백
    private var onPageSelectedCallback: ((position: Int, word: KanjiDetailModel) -> Unit)? = null

    // 핸들러 - UI 스레드에서 지연 처리용
    private val handler = Handler(Looper.getMainLooper())

    // 위치 설정 중 플래그
    private var isSettingPosition = false

    /**
     * ViewPager2 초기 설정
     */
    fun setup(
        words: List<KanjiDetailModel>,
        onPageSelected: (position: Int, word: KanjiDetailModel) -> Unit,
        initialWordPosition: Int
    ) {
        Log.d(TAG, "ViewPager2 설정 시작: ${words.size}개 단어, 초기 위치: $initialWordPosition")

        // 콜백 저장
        this.onPageSelectedCallback = onPageSelected

        // 이전 콜백 정리
        cleanupCallback()

        // 어댑터 설정
        setupAdapter(words)

        // 페이지 변경 콜백 설정
        setupPageChangeCallback(words)

        // ViewPager 최적화
        optimizeViewPager()

        // 초기 위치 설정
        setInitialPosition(words, initialWordPosition)
    }

    /**
     * 페이지 변경 콜백 설정
     */
    private fun setupPageChangeCallback(words: List<KanjiDetailModel>) {
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            private var lastPosition = -1

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // 위치 저장
                currentPosition = position

                // 같은 위치 중복 호출 방지
                if (position == lastPosition && !isSettingPosition) return

                lastPosition = position

                // 범위 확인
                if (position in words.indices) {
                    Log.d(TAG, "페이지 선택: $position (${words[position].kanji})")

                    // 콜백 호출 (UI 스레드에서)
                    handler.post {
                        onPageSelectedCallback?.invoke(position, words[position])
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)

                // 스크롤 종료 시 현재 위치 확인 및 콜백
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    val position = viewPager.currentItem

                    if (position != lastPosition && position in words.indices) {
                        lastPosition = position
                        currentPosition = position

                        Log.d(TAG, "스크롤 종료, 위치 확인: $position")

                        // 스크롤 종료 후 콜백
                        handler.post {
                            onPageSelectedCallback?.invoke(position, words[position])
                        }
                    }
                }
            }
        }

        // 콜백 등록
        pageChangeCallback?.let {
            viewPager.registerOnPageChangeCallback(it)
        }
    }

    /**
     * 어댑터 설정
     */
    private fun setupAdapter(words: List<KanjiDetailModel>) {
        try {
            // 현재 어댑터 제거
            viewPager.adapter = null

            // 오프스크린 페이지 최적화
            viewPager.offscreenPageLimit = 1

            // ViewPager 기본 설정
            viewPager.isUserInputEnabled = true
            viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

            // 어댑터 생성 및 설정
            wordCardAdapter = WordCardAdapter(activity, words)
            viewPager.adapter = wordCardAdapter

            Log.d(TAG, "어댑터 설정 완료: ${words.size}개 단어")
        } catch (e: Exception) {
            Log.e(TAG, "어댑터 설정 실패", e)
        }
    }

    /**
     * 초기 위치 설정
     */
    private fun setInitialPosition(words: List<KanjiDetailModel>, initialPosition: Int) {
        try {
            // 유효한 초기 위치 계산
            val validPosition = initialPosition.coerceIn(0, (words.size - 1).coerceAtLeast(0))

            // 플래그 설정
            isSettingPosition = true

            // ViewPager 초기 위치 설정 (애니메이션 없이)
            viewPager.setCurrentItem(validPosition, false)
            currentPosition = validPosition

            // 초기 위치에 대한 콜백 지연 호출
            handler.postDelayed({
                isSettingPosition = false

                if (validPosition in words.indices) {
                    onPageSelectedCallback?.invoke(validPosition, words[validPosition])
                    Log.d(TAG, "초기 위치 콜백: $validPosition (${words[validPosition].kanji})")
                }
            }, 300)

            Log.d(TAG, "초기 위치 설정: $validPosition")
        } catch (e: Exception) {
            isSettingPosition = false
            Log.e(TAG, "초기 위치 설정 실패", e)
        }
    }

    /**
     * ViewPager 최적화
     */
    private fun optimizeViewPager() {
        try {
            // ViewPager2 내부 RecyclerView 접근
            val recyclerView = getViewPagerRecyclerView() ?: return

            recyclerView.apply {
                // 중첩 스크롤 비활성화
                isNestedScrollingEnabled = false

                // 스크롤 바운스 효과 비활성화
                overScrollMode = RecyclerView.OVER_SCROLL_NEVER

                // 애니메이션 비활성화 (스와이프 문제 해결)
                itemAnimator = null
            }

            Log.d(TAG, "ViewPager 최적화 완료")
        } catch (e: Exception) {
            Log.w(TAG, "ViewPager 최적화 실패", e)
        }
    }

    private fun getViewPagerRecyclerView(): RecyclerView? {
        return try {
            val field = cachedRecyclerViewField ?: run {
                ViewPager2::class.java.getDeclaredField("mRecyclerView").apply {
                    isAccessible = true
                    cachedRecyclerViewField = this
                }
            }

            field.get(viewPager) as? RecyclerView
        } catch (e: Exception) {
            Log.w(TAG, "RecyclerView 접근 실패", e)
            null
        }
    }

    /**
     * 현재 위치 설정
     */
    fun setCurrentItem(position: Int, smoothScroll: Boolean = true) {
        try {
            // 같은 위치 무시
            if (position == currentPosition && viewPager.currentItem == position) {
                return
            }

            // 플래그 설정
            isSettingPosition = true

            Log.d(TAG, "위치 이동: $currentPosition → $position (부드러운 스크롤: $smoothScroll)")

            // ViewPager 위치 변경
            viewPager.setCurrentItem(position, smoothScroll)
            currentPosition = position

            // 플래그 해제
            handler.postDelayed({
                isSettingPosition = false
            }, 300)
        } catch (e: Exception) {
            isSettingPosition = false
            Log.e(TAG, "위치 설정 실패", e)
        }
    }

    /**
     * 현재 위치 반환
     */
    fun getCurrentItem(): Int {
        return viewPager.currentItem
    }

    /**
     * 콜백 정리
     */
    private fun cleanupCallback() {
        pageChangeCallback?.let {
            viewPager.unregisterOnPageChangeCallback(it)
        }
        pageChangeCallback = null
    }

    /**
     * 리소스 정리
     */
    fun release() {
        cleanupCallback()
        handler.removeCallbacksAndMessages(null)
        isSettingPosition = false
    }
}