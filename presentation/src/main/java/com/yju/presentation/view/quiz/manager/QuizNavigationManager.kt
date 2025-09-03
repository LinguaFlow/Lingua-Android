package com.yju.presentation.view.quiz.manager

import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.yju.presentation.R
import com.yju.presentation.view.quiz.ExampleFragment
import com.yju.presentation.view.quiz.KeyboardQuizFragment
import com.yju.presentation.view.quiz.MultipleChoiceQuizFragment
import com.yju.presentation.view.quiz.WordDetailFragment
import com.yju.presentation.view.speech.QuizStep

/**
 * 퀴즈 화면 네비게이션 관리 클래스
 * - 프래그먼트 전환 관리
 * - 백스택 및 백 버튼 콜백 관리
 */
class QuizNavigationManager(
    private val activity: FragmentActivity,
    private val fragmentManager: FragmentManager,
    private val containerId: Int,
    private val onBackStackChanged: () -> Unit
) {
    companion object {
        private const val TAG = "QuizNavigationManager"
    }

    private var currentQuizStep: QuizStep = QuizStep.NONE

    // 백스택 변경 리스너 저장
    private val backStackListener = FragmentManager.OnBackStackChangedListener {
        // 백스택 변경 시 실제 Fragment 상태와 동기화
        syncStepWithCurrentFragment()
        onBackStackChanged()
    }
    // 백 버튼 콜백 참조
    private var backPressedCallback: OnBackPressedCallback? = null

    init {
        // 백스택 변경 리스너 등록
        fragmentManager.addOnBackStackChangedListener(backStackListener)
    }

    /**
     * 현재 Fragment와 QuizStep 동기화
     */
    private fun syncStepWithCurrentFragment() {
        val currentFragment = fragmentManager.findFragmentById(containerId)
        val actualStep = when (currentFragment) {
            is ExampleFragment -> QuizStep.EXAMPLE
            is KeyboardQuizFragment -> QuizStep.KEYBOARD
            is MultipleChoiceQuizFragment -> QuizStep.MULTIPLE_CHOICE
            is WordDetailFragment -> QuizStep.WORD_DETAIL
            else -> QuizStep.NONE
        }

        if (actualStep != currentQuizStep) {
            Log.d(TAG, "Fragment 상태 동기화: $currentQuizStep -> $actualStep (Fragment: ${currentFragment?.javaClass?.simpleName})")
            currentQuizStep = actualStep
        }
    }

    /**
     * 퀴즈 단계 전환
     */
    fun navigateToQuizStep(fragment: Fragment, step: QuizStep, tag: String) {
        Log.d(TAG, "퀴즈 단계 전환 요청: $currentQuizStep -> $step")

        // 먼저 현재 실제 Fragment 상태와 동기화
        syncStepWithCurrentFragment()

        // 현재 Fragment가 요청된 Fragment와 정확히 같은 타입인지 확인
        val currentFragment = fragmentManager.findFragmentById(containerId)
        val isSameFragmentType = when (step) {
            QuizStep.EXAMPLE -> currentFragment is ExampleFragment
            QuizStep.KEYBOARD -> currentFragment is KeyboardQuizFragment
            QuizStep.MULTIPLE_CHOICE -> currentFragment is MultipleChoiceQuizFragment
            QuizStep.WORD_DETAIL -> currentFragment is WordDetailFragment
            QuizStep.NONE -> false
        }

        Log.d(TAG, "동기화 후 상태 - currentStep: $currentQuizStep, fragment: ${currentFragment?.javaClass?.simpleName}, isSameType: $isSameFragmentType")

        // 같은 단계이고 같은 Fragment 타입이면 중복 전환 방지
        if (currentQuizStep == step && isSameFragmentType) {
            Log.d(TAG, "이미 $step 단계에 있고 올바른 Fragment가 표시 중임, 중복 전환 방지")
            return
        }

        try {
            Log.d(TAG, "퀴즈 단계 전환 실행: $currentQuizStep -> $step")
            currentQuizStep = step

            fragmentManager.beginTransaction().apply {
                setCustomAnimations(
                    R.animator.slide_in_right,
                    R.animator.slide_out_left,
                    R.animator.slide_in_left,
                    R.animator.slide_out_right
                )
                setReorderingAllowed(true)
                replace(containerId, fragment)
                addToBackStack(tag)
                commit()
            }

            Log.d(TAG, "퀴즈 단계 전환 완료: $step")

        } catch (e: Exception) {
            Log.e(TAG, "퀴즈 단계 전환 실패: ${e.message}", e)
            // 실패 시 다시 동기화
            syncStepWithCurrentFragment()
        }
    }

    /**
     * 백 버튼 처리 설정
     */
    fun setupBackHandler(onBackPressedAction: () -> Unit) {
        // 기존 콜백 제거
        backPressedCallback?.remove()

        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fragmentManager.backStackEntryCount > 0) {
                    Log.d(TAG, "백 버튼: 프래그먼트 팝")
                    fragmentManager.popBackStack()
                    // syncStepWithCurrentFragment()는 backStackListener에서 자동 호출됨
                } else {
                    Log.d(TAG, "백 버튼: 액티비티 콜백 실행")
                    onBackPressedAction()
                }
            }
        }

        // 백 버튼 콜백 등록
        activity.onBackPressedDispatcher.addCallback(activity, backPressedCallback!!)
        Log.d(TAG, "백 핸들러 설정 완료")
    }

    fun hasBackStack(): Boolean = fragmentManager.backStackEntryCount > 0

    fun getCurrentStep(): QuizStep = currentQuizStep

    /**
     * 현재 단계 설정 (상태 복원 시 사용)
     * 주의: 설정 후 실제 Fragment 상태와 동기화 확인 필요
     */
    fun setCurrentStep(step: QuizStep) {
        Log.d(TAG, "현재 단계 설정: $currentQuizStep -> $step")
        currentQuizStep = step
    }

    /**
     * Fragment 상태와 강제 동기화 (외부 호출용)
     */
    fun forceSyncWithFragment() {
        Log.d(TAG, "Fragment 상태 강제 동기화 요청")
        syncStepWithCurrentFragment()
    }

    /**
     * 리소스 해제: 반드시 Activity.onDestroy()에서 호출
     */
    fun release() {
        fragmentManager.removeOnBackStackChangedListener(backStackListener)
        backPressedCallback?.remove()
        backPressedCallback = null
        Log.d(TAG, "NavigationManager 리소스 해제 완료")
    }
}