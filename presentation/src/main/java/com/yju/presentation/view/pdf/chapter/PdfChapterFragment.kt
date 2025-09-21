package com.yju.presentation.view.pdf.chapter

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.yju.presentation.R
import com.yju.presentation.base.BaseFragment
import com.yju.presentation.databinding.FragmentPdfChapterBinding
import com.yju.presentation.ext.repeatOnStarted
import com.yju.presentation.view.pdf.adapter.ChapterPagerAdapter
import com.yju.presentation.view.pdf.word.PdfWordFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PdfChapterFragment :
    BaseFragment<FragmentPdfChapterBinding, PdfChapterViewModel>(R.layout.fragment_pdf_chapter) {

    // 공유 ViewModel 사용
    override val viewModel: PdfChapterViewModel by activityViewModels()
    private var pagerAdapter: ChapterPagerAdapter? = null
    private var currentPdfId: Long = 0L
    private var tabMediator: TabLayoutMediator? = null
    override val applyTransition: Boolean = true
    private var isInitialized = false
    private var isDataLoading = false
    private var loadDataJob: Job? = null

    companion object {
        private const val TAG = "PdfChapterFragment"
        fun newInstance(pdfId: Long) = PdfChapterFragment().apply {
            arguments = Bundle().apply { putLong("pdf_id", pdfId) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentPdfId = arguments?.getLong("pdf_id", 0L) ?: 0L

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(
                view.paddingLeft,
                statusBarHeight,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        ViewCompat.requestApplyInsets(binding.root)
        if (!isInitialized && currentPdfId > 0) {
            initializeCore()
            setupUI()
            setupObservers()
            isInitialized = true
            loadInitialData(loadKnown = true)
        }
    }

    private fun initializeCore() {
        viewModel.setCurrentPdfId(currentPdfId)
    }

    private fun initViewPager() {
        if (pagerAdapter != null) {
            return
        }

        pagerAdapter = ChapterPagerAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle,
            currentPdfId,
            viewLifecycleOwner.lifecycleScope
        )

        binding.viewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = 1
            registerOnPageChangeCallback(createPageChangeCallback())
        }
    }


    private fun setupUI() {
        initViewPager()
        setupTabLayout()
    }

    private fun setupTabLayout() {
        // 기존 mediator 정리
        tabMediator?.detach()

        tabMediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                ChapterPagerAdapter.TAB_NORMAL -> getString(R.string.tab_normal_chapters)
                ChapterPagerAdapter.TAB_UNKNOWN -> getString(R.string.tab_known_chapters)
                else -> ""
            }
        }
        tabMediator?.attach()
    }

    /**
     * ✅ 옵저버 설정
     */
    private fun setupObservers() = viewLifecycleOwner.repeatOnStarted {
        // 뒤로가기 이벤트
        launch {
            viewModel.onClickBack.collect { shouldGoBack ->
                if (shouldGoBack) {
                    handleBackNavigation()
                }
            }
        }

        // 일반 챕터 선택
        launch {
            viewModel.onClickChapter.collect { (id, chapter) ->
                navigateToPdfWord(id, chapter, false)
            }
        }

        // 모르는 단어 챕터 선택
        launch {
            viewModel.onClickKnownChapter.collect { (id, chapter) ->
                navigateToPdfWord(id, chapter, true)
            }
        }

        // 모르는 단어 탭 새로고침
        launch {
            viewModel.refreshKnownChapters.collect { numAddedWords ->
                handleKnownChaptersRefresh(numAddedWords)
            }
        }

        // 일반 챕터 새로고침
        launch {
            viewModel.refreshNormalChapters.collect { pdfId ->
                if (pdfId == currentPdfId) {
                    pagerAdapter?.refreshPage(ChapterPagerAdapter.TAB_NORMAL)
                }
            }
        }

        // PDF 정보 관찰
        launch {
            viewModel.pdfInfo.observe(viewLifecycleOwner) { info ->
                updateTitle(info?.bookName)
            }
        }

        // 단어 상태 변경
        launch {
            viewModel.wordStatusChanged.collect { (pdfId, _) ->
                if (pdfId == currentPdfId) {
                    delay(100)
                    pagerAdapter?.refreshAllPages()
                }
            }
        }
    }

    /**
     * ✅ 페이지 변경 콜백
     */
    private fun createPageChangeCallback() = object : ViewPager2.OnPageChangeCallback() {
        private var lastPageSelectedTime = 0L
        private val PAGE_CHANGE_THROTTLE = 300L

        override fun onPageSelected(position: Int) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPageSelectedTime < PAGE_CHANGE_THROTTLE) {
                return
            }
            lastPageSelectedTime = currentTime

            viewModel.setCurrentTabPosition(position)
            pagerAdapter?.setCurrentTab(position)
        }
    }

    private fun loadInitialData(loadKnown: Boolean = true) {
        if (isDataLoading) return
        isDataLoading = true

        loadDataJob?.cancel()
        loadDataJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                // PDF 정보와 아는 단어 모두 로드
                viewModel.loadPdfInfo(currentPdfId, loadKnown)

                delay(100) // Fragment 생성 대기

                withContext(Dispatchers.Main) {
                    binding.loadingContainer?.visibility = View.GONE
                    // 모든 페이지 새로고침
                    pagerAdapter?.refreshAllPages()
                }
            } catch (e: Exception) {
                Log.e(TAG, "데이터 로드 실패", e)
                binding.loadingContainer?.visibility = View.GONE
            } finally {
                isDataLoading = false
            }
        }
    }

    /**
     * ✅ 모르는 단어 새로고침 처리
     */
    private fun handleKnownChaptersRefresh(numAddedWords: Long) {
        pagerAdapter?.refreshPage(ChapterPagerAdapter.TAB_UNKNOWN)

        if (numAddedWords > 0 && binding.viewPager.currentItem != ChapterPagerAdapter.TAB_UNKNOWN) {
            binding.viewPager.setCurrentItem(ChapterPagerAdapter.TAB_UNKNOWN, true)
        }
    }

    private fun handleBackNavigation() {
        parentFragmentManager.popBackStack()
    }

    private fun updateTitle(bookName: String?) {
        binding.tvTitle.text = getString(
            R.string.pdf_chapter_title,
            bookName ?: getString(R.string.loading_pdf_chapter)
        )
    }


    private fun navigateToPdfWord(pdfId: Long, chapter: String, isKnown: Boolean) {
        Log.d(TAG, "PDF 단어 화면으로 이동: $pdfId, $chapter, 아는 단어: $isKnown")

        val fragment = PdfWordFragment.newInstance(pdfId, chapter, isKnown)

        parentFragmentManager.commit {
            setReorderingAllowed(true)
            setCustomAnimations(
                R.animator.slide_in_right,
                R.animator.slide_out_left,
                R.animator.slide_in_left,
                R.animator.slide_out_right
            )
            replace(R.id.fragmentContainer, fragment, "PdfWord_${pdfId}_$chapter")
            addToBackStack("PdfWord_${pdfId}_$chapter")
        }
    }

    override fun onDestroyView() {
        loadDataJob?.cancel()
        tabMediator?.detach()
        tabMediator = null
        binding.viewPager.adapter = null
        pagerAdapter?.release()
        pagerAdapter = null
        isInitialized = false
        isDataLoading = false
        super.onDestroyView()
    }
}