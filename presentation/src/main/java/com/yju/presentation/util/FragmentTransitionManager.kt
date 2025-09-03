package com.yju.presentation.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.yju.presentation.R


class FragmentTransitionManager(private val fragmentManager: FragmentManager) {

    fun navigateWithAnimation(
        containerId: Int,
        fragment: Fragment,
        addToBackStack: Boolean = true,
        tag: String? = null,
        withAnimation: Boolean = false // 기본값을 false로 설정하여 애니메이션 비활성화
    ) {
        fragmentManager.beginTransaction().apply {
            // 애니메이션 설정 (요청된 경우에만)
            if (withAnimation) {
                // animator 폴더의 애니메이션 사용
                setCustomAnimations(
                    R.animator.slide_in_right,
                    R.animator.slide_out_left,
                    R.animator.slide_in_left,
                    R.animator.slide_out_right
                )
            }

            // Fragment 교체
            replace(containerId, fragment, tag)

            // 백스택 추가
            if (addToBackStack) {
                addToBackStack(tag)
            }

            // 트랜잭션 최적화
            setReorderingAllowed(true)

            // 커밋
            commit()
        }
    }

    fun navigateWithoutAnimation(
        containerId: Int,
        fragment: Fragment,
        addToBackStack: Boolean = true,
        tag: String? = null
    ) {
        navigateWithAnimation(
            containerId = containerId,
            fragment = fragment,
            addToBackStack = addToBackStack,
            tag = tag,
            withAnimation = false
        )
    }

    fun addWithAnimation(
        containerId: Int,
        fragment: Fragment,
        tag: String? = null,
        withAnimation: Boolean = false
    ) {
        fragmentManager.beginTransaction().apply {
            if (withAnimation) {
                // 카드 플립 애니메이션 사용
                setCustomAnimations(
                    R.animator.card_flip_right_in,
                    R.animator.card_flip_right_out,
                    R.animator.card_flip_left_in,
                    R.animator.card_flip_left_out
                )
            }

            // Fragment 추가
            add(containerId, fragment, tag)
            addToBackStack(tag)

            // 트랜잭션 최적화
            setReorderingAllowed(true)

            // 커밋
            commit()
        }
    }


    fun popBackStackTo(name: String?, flags: Int = 0) {
        fragmentManager.popBackStack(name, flags)
    }

    fun popBackStackImmediate(): Boolean {
        return fragmentManager.popBackStackImmediate()
    }

    fun popBackStackImmediate(name: String?, flags: Int): Boolean {
        return fragmentManager.popBackStackImmediate(name, flags)
    }

    fun clearBackStack() {
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}