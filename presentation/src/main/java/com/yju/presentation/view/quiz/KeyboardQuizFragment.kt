package com.yju.presentation.view.quiz

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.yju.presentation.R
import com.yju.presentation.base.BaseFragment
import com.yju.presentation.databinding.FragmentKeyboardQuizBinding
import com.yju.presentation.ext.repeatOnStarted
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class KeyboardQuizFragment :
    BaseFragment<FragmentKeyboardQuizBinding, KeyboardQuizViewModel>(
        R.layout.fragment_keyboard_quiz
    ) {
    private val sharedViewModel: QuizViewModel by activityViewModels()
    override val applyTransition: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 단어 데이터 설정
        sharedViewModel.word.value?.let { word ->
            viewModel.setWord(word)
        } ?: run {
            showToast("단어 정보를 불러올 수 없습니다")
            requireActivity().finish()
            return
        }
        setupUI()
        setupObservers()
    }

    private fun setupUI() = with(binding) {
        // 사용자가 텍스트 입력할 때마다
        etAnswer.addTextChangedListener {
            viewModel.updateUserInput(it.toString())
        }

        // IME 액션 처리
        etAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.checkAnswer()
                true
            } else false
        }

        btnCheck.setOnClickListener {
            viewModel.checkAnswer()
        }

        btnHint.setOnClickListener {
            viewModel.showHint()
        }

        // 스킵 버튼 → 바로 객관식 퀴즈로 이동
        btnSkip.setOnClickListener {
            (requireActivity() as QuizActivity).navigateToMultipleChoiceQuiz()
        }
    }

    private fun setupObservers() {
        // 단어 데이터 관찰
        viewModel.word.observe(viewLifecycleOwner) { word ->
            binding.tvKanjiInQuiz.text = word.kanji
        }

        // 사용자 입력 관찰
        viewModel.userInput.observe(viewLifecycleOwner) { input ->
            if (binding.etAnswer.text.toString() != input) {
                binding.etAnswer.setText(input)
            }
        }

        // 답안 상태 관찰
        repeatOnStarted {
            viewModel.answerState.collect { state ->
                // 배경 리소스 설정
                val bgRes = when (state) {
                    is KeyboardQuizViewModel.AnswerState.Correct -> R.drawable.bg_input_correct
                    is KeyboardQuizViewModel.AnswerState.Incorrect -> R.drawable.bg_input_incorrect
                    else -> R.drawable.bg_input_default
                }
                binding.etAnswer.setBackgroundResource(bgRes)

                // 정답일 때 → 토스트 후 공유 ViewModel 통과 알림
                if (state is KeyboardQuizViewModel.AnswerState.Correct) {
                    showSuccessToast("정답입니다!")
                    sharedViewModel.onKeyboardQuizPassed()
                }
            }
        }

        // 힌트 이벤트
        repeatOnStarted {
            viewModel.showHintEvent.collect { hint ->
                showToast("힌트: $hint")
            }
        }

        // 퀴즈 통과 이벤트
        repeatOnStarted {
            viewModel.quizPassed.collect {
                sharedViewModel.onKeyboardQuizPassed()
            }
        }
    }

    companion object {
        fun newInstance() = KeyboardQuizFragment()
    }
}