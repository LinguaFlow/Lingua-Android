package com.yju.presentation.view.quiz.card

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.presentation.R
import com.yju.presentation.base.BaseFragment
import com.yju.presentation.databinding.FragmentWordCardBinding
import com.yju.presentation.ext.repeatOnStarted
import com.yju.presentation.view.quiz.QuizActivity
import com.yju.presentation.view.quiz.QuizViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 단어 카드 프래그먼트
 * - 개별 단어 카드 UI 표시
 * - 단어 데이터 관리 및 이벤트 처리
 * - 한자 카드 클릭 시 WordDetail로 직접 이동
 */
@AndroidEntryPoint
class WordCardFragment : BaseFragment<FragmentWordCardBinding, WordCardViewModel>(
    R.layout.fragment_word_card
) {
    companion object {
        private const val TAG = "WordCardFragment"
        private const val ARG_WORD = "word"
        private const val ARG_POSITION = "position"

        @JvmStatic
        fun newInstance(kanjiDetail: KanjiDetailModel, position: Int = -1) =
            WordCardFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_WORD, kanjiDetail)
                    putInt(ARG_POSITION, position)
                }
            }
    }

    private val sharedViewModel: QuizViewModel by activityViewModels()
    private var fragmentPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractWordFromArguments()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWordBinding()
        setupUI()
        observeViewModel()
    }

    /**
     * ViewModel 관찰 설정
     */
    private fun observeViewModel() {
        observeSharedViewModel()
        observeExampleEvent()
    }

    /**
     * 인자에서 단어 데이터 및 위치 추출
     */
    private fun extractWordFromArguments() {
        arguments?.let {
            val word =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    it.getParcelable(ARG_WORD, KanjiDetailModel::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    it.getParcelable(ARG_WORD)
                }
            fragmentPosition = it.getInt(ARG_POSITION, -1)
            word?.let { kanjiDetail ->
                viewModel.setKanjiDetail(kanjiDetail)
                Log.d(TAG, "단어 설정 완료: ${kanjiDetail.kanji}, 위치: $fragmentPosition")
            }
        }
    }

    /**
     * 단어 데이터 바인딩 설정
     */
    private fun setupWordBinding() {
        viewModel.kanjiDetailFlow.value?.let { word ->
            binding.apply {
                this.word = word
                executePendingBindings()
            }
        }
    }

    /**
     * UI 이벤트 설정
     */
    private fun setupUI() {
        // 한자 카드 클릭 시 WordDetail로 직접 이동
        binding.cardKanji.setOnClickListener {
            Log.d(TAG, "한자 카드 클릭: WordDetail로 직접 이동")
            (activity as? QuizActivity)?.navigateToWordDetail()
        }
    }

    /**
     * 공유 ViewModel 관찰
     */
    private fun observeSharedViewModel() {
        sharedViewModel.word.observe(viewLifecycleOwner) { word ->
            // 이 프래그먼트의 데이터와 동일할 때만 UI 갱신
            val myDetail = viewModel.kanjiDetailFlow.value
            if (myDetail != null
                && word.kanji == myDetail.kanji
                && word.vocabularyBookOrder == myDetail.vocabularyBookOrder
            ) {
                updateKanjiCard(word)
            }
        }
    }

    private fun observeExampleEvent() {
        repeatOnStarted {
            viewModel.showExample.collect {
                try {
                    (activity as? QuizActivity)?.navigateToExampleFragment()
                } catch (e: Exception) {
                    Log.e(TAG, "예문 화면 이동 오류: ${e.message}", e)
                }
            }
        }
    }

    private fun updateKanjiCard(kanjiDetail: KanjiDetailModel?) {
        kanjiDetail?.let {
            binding.word = it
        }
    }
}