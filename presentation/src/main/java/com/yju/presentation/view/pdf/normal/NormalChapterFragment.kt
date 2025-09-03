package com.yju.presentation.view.pdf.normal

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yju.presentation.R
import com.yju.presentation.base.BaseFragment
import com.yju.presentation.databinding.FragmentNormalChapterBinding
import com.yju.presentation.ext.repeatOnStarted
import com.yju.presentation.view.pdf.adapter.PdfChapterAdapter
import com.yju.presentation.view.pdf.chapter.PdfChapterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NormalChapterFragment :
    BaseFragment<FragmentNormalChapterBinding, NormalChapterViewModel>(R.layout.fragment_normal_chapter) {

    private val sharedViewModel: PdfChapterViewModel by activityViewModels()
    private lateinit var chapterAdapter: PdfChapterAdapter
    private var currentPdfId: Long = 0L
    override val applyTransition: Boolean = false

    private var isInitialized = false
    private var refreshJob: Job? = null

    companion object {
        private const val TAG = "NormalChapterFragment"
        fun newInstance(pdfId: Long) = NormalChapterFragment().apply {
            arguments = Bundle().apply { putLong("pdf_id", pdfId) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentPdfId = arguments?.getLong("pdf_id", 0L) ?: 0L

        if (currentPdfId > 0 && !isInitialized) {
            isInitialized = true
            setupUI()
            observeData()
            // 초기 데이터는 observe를 통해 자동으로 받음
        }
    }

    private fun setupUI() {
        chapterAdapter = PdfChapterAdapter { chapter ->
            sharedViewModel.onClickChapter(currentPdfId, chapter)
        }

        binding.rvChapter.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chapterAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }
    }

    private fun observeData() = viewLifecycleOwner.repeatOnStarted {
        // PDF 정보 관찰 - 자동으로 챕터 생성
        launch {
            sharedViewModel.pdfInfo.observe(viewLifecycleOwner) { pdfInfo ->
                if (pdfInfo != null && pdfInfo.id == currentPdfId) {
                    Log.d(TAG, "PDF 정보 수신: ${pdfInfo.bookName}")
                    viewModel.setPdfInfo(pdfInfo)
                }
            }
        }

        // 챕터 목록 관찰
        launch {
            viewModel.chapters.collect { chapters ->
                Log.d(TAG, "챕터 목록 업데이트: ${chapters.size}개")
                updateChapterList(chapters)
            }
        }

        // 로딩 상태
        launch {
            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        // 새로고침 이벤트
        launch {
            sharedViewModel.refreshNormalChapters.collect { pdfId ->
                if (pdfId == currentPdfId) {
                    Log.d(TAG, "일반 챕터 새로고침")
                    refreshData()
                }
            }
        }

        // 단어 수 정보
        launch {
            viewModel.wordCountsByPage.observe(viewLifecycleOwner) { wordCounts ->
                chapterAdapter.wordCountsByPage = wordCounts
            }
        }
    }

    private fun updateChapterList(chapters: List<String>) {
        chapterAdapter.submitList(chapters)
        updateEmptyState(chapters.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmptyChapter.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvChapter.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    fun refreshData() {
        refreshJob?.cancel()
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(100)
            viewModel.regenerateChapters()
        }
    }

    override fun onDestroyView() {
        refreshJob?.cancel()
        binding.rvChapter.adapter = null
        isInitialized = false
        super.onDestroyView()
    }
}