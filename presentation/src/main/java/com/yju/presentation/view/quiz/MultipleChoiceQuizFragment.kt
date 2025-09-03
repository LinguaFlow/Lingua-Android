package com.yju.presentation.view.quiz

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.yju.presentation.R
import com.yju.presentation.base.BaseFragment
import com.yju.presentation.databinding.FragmentMultipleChoiceQuizBinding
import com.yju.presentation.ext.repeatOnStarted
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MultipleChoiceQuizFragment : BaseFragment<FragmentMultipleChoiceQuizBinding, MultipleChoiceQuizViewModel>(
    R.layout.fragment_multiple_choice_quiz
) {
    override val applyTransition: Boolean = false
    // 프래그먼트 전용 ViewModel
    override val viewModel: MultipleChoiceQuizViewModel by viewModels()
    // 공유 ViewModel
    private val sharedViewModel: QuizViewModel by activityViewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 단어 데이터 설정
        sharedViewModel.word.value?.let { word ->
            viewModel.setWord(word)
        } ?: run {
            showToast("단어 정보를 불러올 수 없습니다")
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }
        setupUI()
        setupObservers()
    }

    private fun setupUI() = with(binding) {
        // 객관식 버튼 (클릭 리스너 설정)
        btnOptionA.setOnClickListener { viewModel.checkAnswer(0) }
        btnOptionB.setOnClickListener { viewModel.checkAnswer(1) }
        btnOptionC.setOnClickListener { viewModel.checkAnswer(2) }
        btnOptionD.setOnClickListener { viewModel.checkAnswer(3) }

        // 스킵 버튼 → Quiz 메인 화면으로 돌아가기
        btnSkip?.setOnClickListener {
            navigateBackToQuiz()
        }
    }

    private fun setupObservers() {
        // 단어 데이터 관찰
        binding.tvInstruction.text = "이 단어의 의미로 맞는 것을 고르세요"

        // 선택지 텍스트 세팅
        viewModel.options.observe(viewLifecycleOwner) { options ->
            if (options.size == 4) {
                binding.btnOptionA.text = options[0]
                binding.btnOptionB.text = options[1]
                binding.btnOptionC.text = options[2]
                binding.btnOptionD.text = options[3]
            }
        }

        // 정답/오답 하이라이트 및 플로우 전환
        repeatOnStarted {
            viewModel.answerResult.collect { result ->
                when (result) {
                    is MultipleChoiceQuizViewModel.AnswerResult.Correct -> {
                        highlightCorrectOption(result.optionIndex)
                        showSuccessToast("정답입니다!")

                        // 정답 시 공유 ViewModel에 통과 알림 (지연 처리)
                        viewModel.skipQuiz()
                    }
                    is MultipleChoiceQuizViewModel.AnswerResult.Incorrect -> {
                        highlightIncorrectOption(result.optionIndex)
                        showToast("오답입니다. 정답은 ${result.correctOptionIndex + 1}번입니다.")
                    }
                    is MultipleChoiceQuizViewModel.AnswerResult.None -> {
                        resetOptionStyles()
                    }
                }
            }
        }

        // 퀴즈 통과 이벤트
        repeatOnStarted {
            viewModel.quizPassed.collect {
                // 정답 시에도 Quiz 메인 화면으로 돌아가기
                navigateBackToQuiz()
            }
        }
    }

    /**
     * Quiz 메인 화면으로 돌아가기
     * - 모든 백스택을 제거하여 Quiz 화면으로 복귀
     */
    private fun navigateBackToQuiz() {
        // 백 스택에서 모든 프래그먼트 제거
        requireActivity().supportFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }

    // 하이라이트 유틸 (변경 없음)
    private fun highlightCorrectOption(index: Int) {
        resetOptionStyles()
        when (index) {
            0 -> binding.btnOptionA.setBackgroundResource(R.drawable.bg_option_correct)
            1 -> binding.btnOptionB.setBackgroundResource(R.drawable.bg_option_correct)
            2 -> binding.btnOptionC.setBackgroundResource(R.drawable.bg_option_correct)
            3 -> binding.btnOptionD.setBackgroundResource(R.drawable.bg_option_correct)
        }
    }

    private fun highlightIncorrectOption(index: Int) {
        resetOptionStyles()
        when (index) {
            0 -> binding.btnOptionA.setBackgroundResource(R.drawable.bg_option_incorrect)
            1 -> binding.btnOptionB.setBackgroundResource(R.drawable.bg_option_incorrect)
            2 -> binding.btnOptionC.setBackgroundResource(R.drawable.bg_option_incorrect)
            3 -> binding.btnOptionD.setBackgroundResource(R.drawable.bg_option_incorrect)
        }
    }

    private fun resetOptionStyles() {
        binding.btnOptionA.setBackgroundResource(R.drawable.bg_option_default)
        binding.btnOptionB.setBackgroundResource(R.drawable.bg_option_default)
        binding.btnOptionC.setBackgroundResource(R.drawable.bg_option_default)
        binding.btnOptionD.setBackgroundResource(R.drawable.bg_option_default)
    }

    companion object {
        fun newInstance() = MultipleChoiceQuizFragment()
    }
}