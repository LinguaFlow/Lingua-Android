package com.yju.presentation.view.pdf.known

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yju.presentation.R
import com.yju.presentation.base.BaseFragment
import com.yju.presentation.databinding.FragmentChapterListBinding
import com.yju.presentation.ext.repeatOnStarted
import com.yju.presentation.view.pdf.adapter.PdfChapterAdapter
import com.yju.presentation.view.pdf.chapter.PdfChapterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class KnownWordChapterFragment :
    BaseFragment<FragmentChapterListBinding, KnownWordChapterViewModel>(
        R.layout.fragment_chapter_list
    ) {

    override val applyTransition: Boolean = false

    // 독립 ViewModel 사용
    override val viewModel: KnownWordChapterViewModel by viewModels()

    // 공유 ViewModel (챕터 간 통신용)
    private val sharedViewModel: PdfChapterViewModel by activityViewModels()

    private lateinit var chapterAdapter: PdfChapterAdapter
    private var currentPdfId: Long = 0L

    // ✅ 상태 추적 - 중복 방지
    private var isDataLoaded = false
    private var lastRefreshTime = 0L
    private val REFRESH_THROTTLE = 500L // 0.5초
    private var refreshJob: Job? = null

    companion object {
        private const val TAG = "KnownWordChapterFragment"
        fun newInstance(pdfId: Long) = KnownWordChapterFragment().apply {
            arguments = Bundle().apply { putLong("pdf_id", pdfId) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentPdfId = arguments?.getLong("pdf_id", 0L) ?: 0L

        if (currentPdfId > 0) {
            viewModel.setPdfId(currentPdfId)
            setupUI()
            setupObservers()
            // 초기 로드는 한 번만
            if (!isDataLoaded) {
                loadInitialData()
            }
        }
    }

    /**
     * ✅ UI 초기화 - 간소화
     */
    private fun setupUI() {
        Log.d(TAG, "RecyclerView 어댑터 설정")

        chapterAdapter = PdfChapterAdapter { chapter ->
            handleChapterClick(chapter)
        }

        binding.rvChapters.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chapterAdapter
            setHasFixedSize(true)
        }

        // 빈 메시지 텍스트 설정
        binding.tvEmptyMessage.text = getString(R.string.msg_no_known)
    }

    /**
     * ✅ 옵저버 설정 - 최적화
     */
    private fun setupObservers() = viewLifecycleOwner.repeatOnStarted {
        // 독립 ViewModel의 챕터 목록 관찰
        launch {
            viewModel.knownChapters.observe(viewLifecycleOwner) { chapters ->
                Log.d(TAG, "아는 단어 챕터 목록 변경: ${chapters.size}개")
                updateChapterList(chapters)
            }
        }

        // 네비게이션 이벤트
        launch {
            viewModel.navigateToKnownWord.collect { (id, chapter) ->
                Log.d(TAG, "아는 단어 네비게이션: PDF ID $id, 챕터 $chapter")
                sharedViewModel.onClickKnownChapter(id, chapter)
            }
        }

        // 공유 ViewModel의 새로고침 이벤트 - 쓰로틀링 적용
        launch {
            sharedViewModel.refreshKnownChapters.collect { numAddedWords ->
                Log.d(TAG, "공유 ViewModel에서 새로고침 이벤트 수신: $numAddedWords")
                refreshDataWithThrottle()
            }
        }

        // 단어 상태 변경 이벤트 - 쓰로틀링 적용
        launch {
            sharedViewModel.wordStatusChanged.collect { (changedPdfId, _) ->
                if (changedPdfId == currentPdfId) {
                    Log.d(TAG, "단어 상태 변경 이벤트 수신")
                    refreshDataWithThrottle()
                }
            }
        }
    }

    /**
     * ✅ 초기 데이터 로드 - 한 번만
     */
    private fun loadInitialData() {
        if (isDataLoaded) return

        Log.d(TAG, "초기 데이터 로드 시작")
        isDataLoaded = true

        // sharedViewModel이 로드될 때까지 대기
        viewLifecycleOwner.lifecycleScope.launch {
            // PDF 정보가 로드될 때까지 대기
            sharedViewModel.pdfInfo.observe(viewLifecycleOwner) { pdfInfo ->
                if (pdfInfo != null && pdfInfo.id == currentPdfId) {
                    // 아는 단어 가져오기
                    val knownWords = sharedViewModel.getKnownWords(currentPdfId)
                    viewModel.updateKnownWords(knownWords)
                }
            }
        }
    }

    /**
     * ✅ 챕터 목록 업데이트 - 간소화
     */
    private fun updateChapterList(chapters: List<String>) {
        Log.d(TAG, "챕터 목록 업데이트: ${chapters.size}개")

        // 어댑터에 데이터 설정
        chapterAdapter.submitList(chapters)

        // 페이지별 단어 수 설정
        if (chapters.isNotEmpty()) {
            val wordCountsByPage = chapters.mapIndexed { index, chapter ->
                index to viewModel.getWordCountForChapter(chapter)
            }.toMap()
            chapterAdapter.wordCountsByPage = wordCountsByPage
        }

        // 빈 상태 처리
        updateEmptyState(chapters.isEmpty())
    }

    /**
     * ✅ 빈 상태 업데이트
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmptyMessage.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvChapters.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    /**
     * ✅ 챕터 클릭 처리
     */
    private fun handleChapterClick(chapter: String) {
        Log.d(TAG, "챕터 클릭: $chapter")
        viewModel.onClickKnownChapter(currentPdfId, chapter)
    }

    /**
     * ✅ 데이터 새로고침 - 외부 호출용
     */
    fun refreshData() {
        Log.d(TAG, "refreshData 호출됨")
        refreshDataWithThrottle()
    }

    /**
     * ✅ 쓰로틀링을 적용한 새로고침
     */
    private fun refreshDataWithThrottle() {
        val currentTime = System.currentTimeMillis()

        // 쓰로틀링 체크
        if (currentTime - lastRefreshTime < REFRESH_THROTTLE) {
            Log.d(TAG, "새로고침 쓰로틀링")
            return
        }

        lastRefreshTime = currentTime

        // 기존 작업 취소
        refreshJob?.cancel()

        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            // 짧은 디바운싱
            delay(100)

            // 공유 ViewModel에서 최신 데이터 가져오기
            val knownWords = sharedViewModel.getKnownWords(currentPdfId)
            viewModel.updateKnownWords(knownWords)
        }
    }

    override fun onResume() {
        super.onResume()

        // ✅ 필요한 경우에만 새로고침
        if (isDataLoaded && viewModel.knownChapters.value?.isEmpty() == true) {
            Log.d(TAG, "onResume - 챕터가 비어있어 새로고침")
            refreshDataWithThrottle()
        } else {
            Log.d(TAG, "onResume - 데이터가 이미 있음")
        }
    }

    override fun onDestroyView() {
        // 작업 취소
        refreshJob?.cancel()

        // 어댑터 정리
        binding.rvChapters.adapter = null

        // 상태 초기화
        isDataLoaded = false

        Log.d(TAG, "onDestroyView - 리소스 정리")
        super.onDestroyView()
    }
}