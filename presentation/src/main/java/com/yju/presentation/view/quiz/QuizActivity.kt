package com.yju.presentation.view.quiz

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.presentation.R
import com.yju.presentation.base.BaseActivity
import com.yju.presentation.databinding.ActivityQuizBinding
import com.yju.presentation.ext.repeatOnStarted
import com.yju.presentation.util.QuizMenu
import com.yju.presentation.view.quiz.manager.PageIndicatorManager
import com.yju.presentation.view.quiz.manager.ViewPager2Manager
import com.yju.presentation.view.quiz.manager.AudioManager
import com.yju.presentation.view.quiz.manager.QuizNavigationManager
import com.yju.presentation.view.quiz.sheet.QuizMenuBottomSheetFragment
import com.yju.presentation.view.speech.QuizStep
import com.yju.presentation.view.speech.TTSManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QuizActivity : BaseActivity<ActivityQuizBinding, QuizViewModel>(R.layout.activity_quiz) {
    @Inject
    lateinit var ttsManager: TTSManager
    private lateinit var indicatorManager: PageIndicatorManager
    private lateinit var viewPagerManager: ViewPager2Manager
    private lateinit var audioManager: AudioManager
    private lateinit var navigationManager: QuizNavigationManager
    private var wordsList: List<KanjiDetailModel> = emptyList()
    private var cachedBottomSheet: QuizMenuBottomSheetFragment? = null
    private var isFirstLaunch = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFirstLaunch = savedInstanceState == null
        initializeManagers()
        restoreState(savedInstanceState)
        if (!loadWordData()) return
        setupViewPager()
        setupUI()
        setupObservers()
        indicatorManager.createIndicators(wordsList.size)
        setInitialUIState()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(50)
            navigationManager.forceSyncWithFragment()
            updateUIForBackStackChange()
            Log.d("QuizActivity", "onResume: 상태 동기화 및 UI 업데이트 완료")
        }
    }

    /**
     * 매니저 컴포넌트 초기화
     */
    private fun initializeManagers() {
        // 인디케이터 관리자 초기화
        indicatorManager = PageIndicatorManager(this, binding.pageIndicator)
        // ViewPager 관리자 초기화
        viewPagerManager = ViewPager2Manager(this, binding.viewPager)
        // 오디오 관리자 초기화
        audioManager = AudioManager(ttsManager, this)
        audioManager.initialize(lazy = true)
        // 오디오 시각화 설정
        binding.audioVisualizer.let { visualizer ->
            audioManager.setupVisualizer(
                visualizer.audioWaveContainer,
                visualizer.audioWaveView,
                visualizer.tvAudioDuration
            )
        }
        navigationManager = QuizNavigationManager(
            this,
            supportFragmentManager,
            R.id.quizContainer,
            this::updateUIForBackStackChange
        )
        navigationManager.setupBackHandler {
            audioManager.stopPlayback()
            finish()
        }
    }

    /**
     * 상태 복원 (Fragment는 Android가 자동 복원)
     */
    private fun restoreState(savedInstanceState: Bundle?) {
        savedInstanceState?.let { state ->
            state.getString("quiz_step")?.let { stepName ->
                try {
                    val restoredStep = QuizStep.valueOf(stepName)
                    Log.d("QuizActivity", "퀴즈 단계 복원: $stepName")
                    navigationManager.setCurrentStep(restoredStep)
                } catch (e: IllegalArgumentException) {
                    Log.w("QuizActivity", "알 수 없는 퀴즈 단계: $stepName")
                    navigationManager.setCurrentStep(QuizStep.NONE)
                }
            }
        }
    }

    /**
     * 초기 UI 상태 설정 (Fragment 없는 상태 기준)
     */
    private fun setInitialUIState() {
        binding.apply {
            // 메인 UI 요소는 기본적으로 표시
            toolbarLayout.visibility = View.VISIBLE
            viewPager.visibility = View.VISIBLE
            pageIndicator.visibility = if (wordsList.size <= 1) View.GONE else View.VISIBLE
            btnPlay.visibility = View.VISIBLE
            quizContainer.visibility = View.GONE
            quizContainer.elevation = 0f
        }
        Log.d("QuizActivity", "초기 UI 상태 설정 완료")
    }

    /**
     * 단어 데이터 로드
     */
    private fun loadWordData(): Boolean {
        Log.d("QuizActivity", "단어 데이터 로드 시작")
        val currentWord: KanjiDetailModel? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra("word", KanjiDetailModel::class.java)
            else
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("word")

        val allWords: ArrayList<KanjiDetailModel>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableArrayListExtra("words", KanjiDetailModel::class.java)
            else
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra("words")
        if (allWords.isNullOrEmpty()) {
            showToast("단어 정보를 불러올 수 없습니다")
            Log.e("QuizActivity", "단어 목록이 없거나 비어있음")
            finish()
            return false
        }

        // 단어 목록 설정
        wordsList = allWords
        viewModel.setWords(wordsList)
        Log.d("QuizActivity", "단어 목록 로드: ${wordsList.size}개")

        // 현재 단어 설정
        currentWord?.let {
            viewModel.setWord(it)
            Log.d("QuizActivity", "현재 단어 설정: ${it.kanji}")
        }
        return true
    }


    private fun setupViewPager() {
        try {
            Log.d("QuizActivity", "ViewPager 설정 시작")
            val initialPosition = findInitialPosition()

            // 인디케이터 먼저 생성
            indicatorManager.clear()
            indicatorManager.createIndicators(wordsList.size)

            // ViewPager 설정
            viewPagerManager.setup(
                words = wordsList,
                onPageSelected = { position, word ->
                    // 단어가 현재와 다른 경우에만 ViewModel 업데이트
                    val currentWord = viewModel.word.value
                    if (currentWord?.kanji != word.kanji ||
                        currentWord.vocabularyBookOrder != word.vocabularyBookOrder
                    ) {
                        Log.d("QuizActivity", "페이지 변경: $position → ${word.kanji}")
                        viewModel.setWord(word)
                        viewModel.setCurrentPosition(position)
                    }

                    // 인디케이터 업데이트
                    indicatorManager.updateIndicators(position, wordsList.size)
                },
                initialWordPosition = initialPosition
            )

            // 다중 단어일 때만 안내 표시 (첫 실행 시에만)
            if (wordsList.size > 1 && isFirstLaunch) {
                Toast.makeText(this, "← 좌우로 스와이프하여 단어 이동 →", Toast.LENGTH_SHORT).show()
            }

            // 초기 인디케이터 업데이트
            indicatorManager.updateIndicators(initialPosition, wordsList.size)

            Log.d("QuizActivity", "ViewPager 설정 완료")
        } catch (e: Exception) {
            Log.e("QuizActivity", "ViewPager 설정 오류", e)
            showToast("ViewPager 설정 중 오류가 발생했습니다")
        }
    }

    private fun findInitialPosition(): Int {
        val currentWord = viewModel.word.value ?: return 0

        val exactMatch = wordsList.indexOfFirst {
            it.kanji == currentWord.kanji && it.vocabularyBookOrder == currentWord.vocabularyBookOrder
        }

        if (exactMatch >= 0) {
            Log.d("QuizActivity", "초기 위치: 정확히 일치하는 단어 - 위치 $exactMatch")
            return exactMatch
        }

        val kanjiMatch = wordsList.indexOfFirst { it.kanji == currentWord.kanji }
        if (kanjiMatch >= 0) {
            Log.d("QuizActivity", "초기 위치: 한자만 일치하는 단어 - 위치 $kanjiMatch")
            return kanjiMatch
        }

        Log.d("QuizActivity", "초기 위치: 일치하는 단어 없음 - 첫 번째 단어(0)")
        return 0
    }

    private fun setupUI() {
        Log.d("QuizActivity", "UI 이벤트 리스너 설정")
        binding.btnPlay.setOnClickListener {
            viewModel.onPlayAudio()
        }

        binding.btnMenu.setOnClickListener {
            viewModel.onClickMenu()
        }
    }

    private fun setupObservers() {
        Log.d("QuizActivity", "ViewModel 관찰 설정")
        viewModel.currentPosition.observe(this) { position ->
            if (viewPagerManager.getCurrentItem() != position) {
                Log.d("QuizActivity", "위치 업데이트: $position")
                viewPagerManager.setCurrentItem(position)
            }
        }
        viewModel.isPlaying.observe(this) { isPlaying ->
            binding.btnPlay.isSelected = isPlaying
        }
        setupEventObservers()
    }

    private fun setupEventObservers() {
        repeatOnStarted {
            viewModel.back.collect {
                Log.d("QuizActivity", "뒤로가기 이벤트")
                onBackPressedDispatcher.onBackPressed()
            }
        }
        repeatOnStarted {
            viewModel.showMenu.collect {
                Log.d("QuizActivity", "메뉴 버튼 이벤트")
                showQuizMenuBottomSheet()
            }
        }
        repeatOnStarted {
            viewModel.playAudio.collect { furigana ->
                Log.d("QuizActivity", "오디오 재생 이벤트: $furigana")
                playAudio(furigana)
            }
        }
        repeatOnStarted {
            viewModel.stopAudio.collect {
                Log.d("QuizActivity", "오디오 중지 이벤트")
                stopAllAudio()
            }
        }
        repeatOnStarted {
            viewModel.keyboardQuizPassed.collect {
                Log.d("QuizActivity", "키보드 퀴즈 통과 이벤트")
                navigateToMultipleChoiceQuiz()
            }
        }
        repeatOnStarted {
            viewModel.multipleChoiceQuizPassed.collect {
                Log.d("QuizActivity", "객관식 퀴즈 통과 이벤트")
                navigateToWordDetail()
            }
        }
    }

    private fun playAudio(text: String) {
        val success = audioManager.playText(text)
        if (success) {
            binding.audioVisualizer.audioWaveContainer.visibility = View.VISIBLE
            showToast("발음 반복 재생 시작")
        } else {
            showToast("발음 재생에 실패했습니다")
            viewModel.stopAudio()
        }
    }


    fun stopAllAudio() {
        audioManager.stopPlayback()
        binding.audioVisualizer.audioWaveContainer.visibility = View.GONE

        if (viewModel.isPlaying.value == true) {
            viewModel.stopAudio()
        }
    }

    /**
     * 백스택 변화에 따른 UI 업데이트
     */
    private fun updateUIForBackStackChange() {
        val hasBackStack = navigationManager.hasBackStack()
        Log.d(
            "QuizActivity",
            "UI 업데이트: 백스택 ${if (hasBackStack) "있음" else "없음"}, 현재 단계: ${navigationManager.getCurrentStep()}"
        )

        binding.apply {
            // 메인 UI 요소 표시/숨김 설정
            toolbarLayout.visibility = if (hasBackStack) View.GONE else View.VISIBLE
            viewPager.visibility = if (hasBackStack) View.GONE else View.VISIBLE
            pageIndicator.visibility =
                if (hasBackStack || wordsList.size <= 1) View.GONE else View.VISIBLE
            btnPlay.visibility = if (hasBackStack) View.GONE else View.VISIBLE

            // 프래그먼트 컨테이너 설정
            if (hasBackStack) {
                quizContainer.visibility = View.VISIBLE
                quizContainer.elevation = 10f
            } else {
                quizContainer.visibility = View.GONE
                quizContainer.elevation = 0f
            }
        }
    }

    fun navigateToExampleFragment() {
        Log.d("QuizActivity", "예문 화면으로 전환")
        stopAllAudio()
        dismissBottomSheetIfShowing()
        navigationManager.navigateToQuizStep(ExampleFragment(), QuizStep.EXAMPLE, "Example")
    }

    internal fun navigateToKeyboardQuiz() {
        Log.d("QuizActivity", "키보드 퀴즈 화면으로 전환")
        stopAllAudio()
        dismissBottomSheetIfShowing()
        lifecycleScope.launch {
            delay(100)
            navigationManager.navigateToQuizStep(
                KeyboardQuizFragment.newInstance(),
                QuizStep.KEYBOARD,
                "KeyboardQuiz"
            )
        }
    }

    internal fun navigateToMultipleChoiceQuiz() {
        Log.d("QuizActivity", "객관식 퀴즈 화면으로 전환")
        stopAllAudio()
        dismissBottomSheetIfShowing()
        lifecycleScope.launch {
            delay(100)
            navigationManager.navigateToQuizStep(
                MultipleChoiceQuizFragment.newInstance(),
                QuizStep.MULTIPLE_CHOICE,
                "MultipleChoiceQuiz"
            )
        }
    }

    fun navigateToWordDetail() {
        Log.d("QuizActivity", "단어 상세 화면으로 전환")
        stopAllAudio()
        dismissBottomSheetIfShowing()
        lifecycleScope.launch {
            delay(100)
            navigationManager.navigateToQuizStep(
                WordDetailFragment.newInstance(),
                QuizStep.WORD_DETAIL,
                "WordDetail"
            )
        }
    }

    fun showMainViewWithAnimation() {
        Log.d("QuizActivity", "메인 뷰 애니메이션 시작")

        binding.apply {
            val duration = 300L
            val animations = listOf(
                toolbarLayout,
                viewPager,
                pageIndicator,
                btnPlay
            ).filter { it.isVisible }
                .map { view ->
                    ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
                        this.duration = duration
                        interpolator = DecelerateInterpolator()
                    }
                }
            AnimatorSet().apply {
                playTogether(animations)
                start()
            }
        }
    }

    fun getCurrentWord(): KanjiDetailModel? = viewModel.word.value

    fun prepareMainView() {
        binding.apply {
            toolbarLayout.visibility = View.VISIBLE
            viewPager.visibility = View.VISIBLE
            pageIndicator.visibility = if (wordsList.size <= 1) View.GONE else View.VISIBLE
            btnPlay.visibility = View.VISIBLE
            toolbarLayout.alpha = 0f
            viewPager.alpha = 0f
            pageIndicator.alpha = 0f
            btnPlay.alpha = 0f
        }
    }

    private fun dismissBottomSheetIfShowing() {
        cachedBottomSheet?.takeIf { it.isAdded && it.showsDialog }?.dismiss()
    }

    private fun showQuizMenuBottomSheet() {
        if (cachedBottomSheet?.isAdded == true) return

        supportFragmentManager.findFragmentByTag(QuizMenuBottomSheetFragment::class.java.name)
            ?.let { fragment ->
                if (fragment is QuizMenuBottomSheetFragment) {
                    fragment.dismiss()
                }
            }

        cachedBottomSheet = QuizMenuBottomSheetFragment().apply {
            onMenuSelected = { menu ->
                when (menu) {
                    QuizMenu.KEYBOARD -> navigateToKeyboardQuiz()
                    QuizMenu.MULTIPLE_CHOICE -> navigateToMultipleChoiceQuiz()
                    QuizMenu.WORD_DETAIL -> navigateToWordDetail()
                    QuizMenu.EXAMPLE -> navigateToExampleFragment()
                }
            }
        }
        cachedBottomSheet?.show(
            supportFragmentManager,
            QuizMenuBottomSheetFragment::class.java.name
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("quiz_step", navigationManager.getCurrentStep().name)
        outState.putInt("current_position", viewPagerManager.getCurrentItem())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (wordsList.isNotEmpty()) {
            val position = savedInstanceState.getInt("current_position", 0)
            if (position in wordsList.indices) {
                viewPagerManager.setCurrentItem(position, false)
                Log.d("QuizActivity", "위치 복원: $position")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewPagerManager.release()
        indicatorManager.clear()
        audioManager.stopPlayback()
        navigationManager.release()
        cachedBottomSheet = null
    }
}