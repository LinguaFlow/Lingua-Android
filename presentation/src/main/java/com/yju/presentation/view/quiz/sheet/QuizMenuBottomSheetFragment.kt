package com.yju.presentation.view.quiz.sheet

import android.os.Bundle
import android.util.Log
import android.view.View
import com.yju.presentation.R
import com.yju.presentation.base.BaseBottomSheetDialogFragment
import com.yju.presentation.databinding.FragmentQuizMenuBottomSheetBinding
import com.yju.presentation.ext.repeatOnStarted
import com.yju.presentation.util.QuizMenu
import dagger.hilt.android.AndroidEntryPoint

/**
 * 퀴즈 메뉴 바텀시트 프래그먼트
 * - 모든 퀴즈 화면으로의 진입점 역할
 * - WordCard의 한자 카드 클릭 시나 메뉴 버튼 클릭 시 표시
 */
@AndroidEntryPoint
class QuizMenuBottomSheetFragment :
    BaseBottomSheetDialogFragment<FragmentQuizMenuBottomSheetBinding, QuizMenuBottomSheetViewModel>(
        R.layout.fragment_quiz_menu_bottom_sheet
    ) {

    var onMenuSelected: ((QuizMenu) -> Unit)? = null
    override fun getTheme() = R.style.BottomSheetDialogTheme

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectEvents()
    }

    private fun collectEvents() {
        // 키보드 퀴즈 선택 이벤트
        repeatOnStarted {
            viewModel.keyboardQuizSelected.collect {
                Log.d("QuizMenuBottomSheet", "키보드 퀴즈 선택 이벤트 수신")
                onMenuSelected?.invoke(QuizMenu.KEYBOARD)
                dismiss()
            }
        }

        // 객관식 퀴즈 선택 이벤트
        repeatOnStarted {
            viewModel.multipleChoiceQuizSelected.collect {
                Log.d("QuizMenuBottomSheet", "객관식 퀴즈 선택 이벤트 수신")
                onMenuSelected?.invoke(QuizMenu.MULTIPLE_CHOICE)
                dismiss()
            }
        }

        // 단어 상세 선택 이벤트
        repeatOnStarted {
            viewModel.wordDetailSelected.collect {
                Log.d("QuizMenuBottomSheet", "단어 상세 선택 이벤트 수신")
                onMenuSelected?.invoke(QuizMenu.WORD_DETAIL)
                dismiss()
            }
        }

        // 예문 선택 이벤트
        repeatOnStarted {
            viewModel.exampleSelected.collect {
                Log.d("QuizMenuBottomSheet", "예문 선택 이벤트 수신")
                onMenuSelected?.invoke(QuizMenu.EXAMPLE)
                dismiss()
            }
        }
    }
}