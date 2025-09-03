package com.yju.presentation.view.quiz

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.presentation.R
import com.yju.presentation.base.BaseFragment
import com.yju.presentation.databinding.FragmentExampleBinding
import com.yju.presentation.ext.repeatOnStarted
import com.yju.presentation.view.pdf.adapter.ExampleAdapter
import com.yju.presentation.view.speech.TTSManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ExampleFragment : BaseFragment<FragmentExampleBinding, ExampleViewModel>(
    R.layout.fragment_example
) {
    @Inject
    lateinit var ttsManager: TTSManager
    override val applyTransition: Boolean = false
    private lateinit var exampleAdapter: ExampleAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        view.post {
            initializeWordData()
        }
    }
    private fun initializeWordData() {
        (requireActivity() as? QuizActivity)?.let { activity ->
            activity.getCurrentWord()?.let { word ->
                viewModel.setWord(word)
                updateWordInfo(word)
            } ?: run {
                showToast("단어 정보를 불러올 수 없습니다")
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        } ?: run {
            showToast("화면 오류가 발생했습니다")
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun updateWordInfo(word: KanjiDetailModel) {
        binding.tvCurrentWord.text = word.kanji
        binding.tvCurrentMeaning.text = word.means
    }

    private fun setupRecyclerView() {
        exampleAdapter = ExampleAdapter { japaneseText ->
            viewModel.onClickPlayAudio(japaneseText)
        }

        binding.rvExamples.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exampleAdapter
            setHasFixedSize(true)
        }
    }

    /**
     * 이벤트 옵저버 설정
     */
    private fun setupObservers() {
        repeatOnStarted {
            viewModel.onClickBack.collect {
                if (it) {
                    stopAudioAndTTS()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        // 오디오 재생 이벤트
        repeatOnStarted {
            viewModel.onClickPlayAudio.collect { text ->
                if (text.isNotEmpty()) {
                    (requireActivity() as? QuizActivity)?.ttsManager?.stopAllPlayback()

                    val success = ttsManager.playTextRepeatedly(text, requireActivity())
                    if (success) {
                        binding.audioVisualizer.audioWaveContainer.visibility = View.VISIBLE
                        showToast("예문 발음 재생 시작")
                    } else {
                        showToast("발음 재생에 실패했습니다")
                        viewModel.onClickStopAudio()
                    }
                }
            }
        }

        repeatOnStarted {
            viewModel.onClickStopAudio.collect {
                if (it) {
                    stopAudioAndTTS()
                }
            }
        }

        viewModel.translationExamples.observe(viewLifecycleOwner) { examplesModel ->
            if (examplesModel?.examples?.isNotEmpty() == true) {
                binding.tvNoExamples.visibility = View.GONE
                binding.rvExamples.visibility = View.VISIBLE
                exampleAdapter.submitList(examplesModel.examples)
            } else {
                binding.tvNoExamples.visibility = View.VISIBLE
                binding.rvExamples.visibility = View.GONE
            }
        }

        // 재생 상태 관찰
        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            exampleAdapter.setAudioPlayingState(isPlaying)
            if (!isPlaying) {
                binding.audioVisualizer.audioWaveContainer.visibility = View.GONE
            }
        }
    }

    private fun stopAudioAndTTS() {
        ttsManager.stopAllPlayback()  // 모든 재생 중지
        binding.audioVisualizer.audioWaveContainer.visibility = View.GONE

        // ViewModel 상태가 재생 중이면 업데이트
        if (viewModel.isPlaying.value == true) {
            viewModel.onClickStopAudio()
        }
    }
}