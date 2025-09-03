package com.yju.presentation.view.quiz

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.yju.presentation.R
import com.yju.presentation.base.BaseFragment
import com.yju.presentation.databinding.FragmentWordDetailBinding
import com.yju.presentation.ext.repeatOnStarted
import com.yju.presentation.view.speech.TTSManager
import com.yju.presentation.view.speech.bindToFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WordDetailFragment : BaseFragment<FragmentWordDetailBinding, WordDetailViewModel>(
    R.layout.fragment_word_detail
) {
    @Inject
    lateinit var ttsManager: TTSManager
    override val applyTransition: Boolean = false
    private val sharedViewModel: QuizViewModel by activityViewModels()
    private var isAnimating = false
    private val animationDuration = 450L

    companion object {
        fun newInstance() = WordDetailFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.vm = viewModel
        initWordData() ?: return
        binding.cardContent.cameraDistance = 12000f
        setupTTS()
        setupListeners()
        observeEvents()
    }

    private fun initWordData(): Boolean? {
        return sharedViewModel.word.value?.let { word ->
            viewModel.setWord(word)
            true
        } ?: run {
            showToast("단어 정보를 불러올 수 없습니다")
            requireActivity().finish()
            null
        }
    }

    private fun setupTTS() {
        binding.audioVisualizer.let { visualizer ->
            ttsManager.bindToFragment(
                fragment = this,
                container = visualizer.audioWaveContainer,
                waveView = visualizer.audioWaveView,
                durationText = visualizer.tvAudioDuration
            )
        }
    }

    private fun setupListeners() = with(binding) {
        btnBack.setOnClickListener { navigateBackWithAnimation() }
        btnMenu.setOnClickListener { sharedViewModel.onClickMenu() }
        btnAudio.setOnClickListener {
            if (viewModel.isPlaying.value == true) {
                stopAudio()  // 이미 재생 중이면 바로 중지
            } else {
                viewModel.toggleAudio()  // 재생 중이 아니면 토글 (재생 시작)
            }
        }
        cardContent.setOnClickListener {
            navigateBackWithAnimation()
        }
        detailContainer.setOnClickListener {
            navigateBackWithAnimation()
        }
    }

    private fun observeEvents() {
        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            binding.btnAudio.isSelected = isPlaying
        }
        repeatOnStarted {
            viewModel.playAudioEvent.collect { furigana ->
                handlePlayAudioEvent(furigana)
            }
        }
        repeatOnStarted {
            viewModel.stopAudioEvent.collect {
                stopAudio()
            }
        }
        repeatOnStarted {
            viewModel.back.collect {
                navigateBackWithAnimation()
            }
        }
    }

    private fun handlePlayAudioEvent(furigana: String) {
        if (furigana.isNotEmpty()) {
            val success = ttsManager.playTextRepeatedly(furigana, this@WordDetailFragment)

            if (success) {
                binding.audioVisualizer.audioWaveContainer.visibility = View.VISIBLE
                showToast("발음 반복 재생 시작")
            } else {
                viewModel.stopAudio()  // 여기서 ViewModel에 알림
                showToast("발음 재생에 실패했습니다")
            }
        }
    }


    private fun stopAudio() {
        ttsManager.stopPlayback(this@WordDetailFragment)
        binding.audioVisualizer.audioWaveContainer.visibility = View.GONE
        if (viewModel.isPlaying.value == true) {
            viewModel.stopAudio()
        }
    }

    private fun navigateBackWithAnimation() {
        if (isAnimating) return
        ttsManager.stopAllPlayback()
        viewModel.stopAudio()
        performFlipAnimation()
    }


    private fun performFlipAnimation() {
        val quizActivity = requireActivity() as QuizActivity
        isAnimating = true
        val card = binding.cardContent
        card.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        quizActivity.prepareMainView()
        val flipOutAnimator = ObjectAnimator.ofFloat(card, View.ROTATION_Y, 0f, 90f).apply {
            duration = animationDuration / 2
            interpolator = AccelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    navigateBackToQuiz()
                }
            })
        }
        flipOutAnimator.start()
    }

    private fun navigateBackToQuiz() {
        ttsManager.stopAllPlayback()
        requireActivity().supportFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        val quizActivity = requireActivity() as QuizActivity
        quizActivity.showMainViewWithAnimation()
        isAnimating = false
    }
}