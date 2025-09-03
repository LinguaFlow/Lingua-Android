package com.yju.presentation.view.pdf.word

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.presentation.R
import com.yju.presentation.base.BaseFragment
import com.yju.presentation.databinding.FragmentPdfWordBinding
import com.yju.presentation.ext.repeatOnStarted
import com.yju.presentation.view.pdf.adapter.PdfWordAdapter
import com.yju.presentation.view.pdf.chapter.PdfChapterViewModel
import com.yju.presentation.view.quiz.QuizActivity
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PdfWordFragment :
    BaseFragment<FragmentPdfWordBinding, PdfWordViewModel>(R.layout.fragment_pdf_word) {
    override val viewModel: PdfWordViewModel by viewModels()
    private val sharedViewModel: PdfChapterViewModel by activityViewModels()
    private val pdfId by lazy { requireArguments().getLong(ARG_PDF_ID) }
    private val chapterTitle by lazy { requireArguments().getString(ARG_CHAPTER_TITLE).orEmpty() }
    private val isKnown by lazy { requireArguments().getBoolean(ARG_IS_KNOWN) }
    override val applyTransition: Boolean = false

    companion object {
        private const val TAG = "PdfWordFragment"
        private const val ARG_PDF_ID = "pdf_id"
        private const val ARG_CHAPTER_TITLE = "chapter_title"
        private const val ARG_IS_KNOWN = "is_known"

        fun newInstance(
            pdfId: Long,
            chapterTitle: String,
            isKnown: Boolean = false
        ) = PdfWordFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_PDF_ID, pdfId)
                putString(ARG_CHAPTER_TITLE, chapterTitle)
                putBoolean(ARG_IS_KNOWN, isKnown)
            }
        }
    }

    // Adapter
    private val wordAdapter: PdfWordAdapter by lazy {
        PdfWordAdapter(
            onItemClick = ::handleWordClick,
            onItemLongClick = { word, enteringSelectionMode ->
                Log.d(TAG, "롱클릭 이벤트 수신: ${word.kanji}, 선택 모드 진입: $enteringSelectionMode")
                if (enteringSelectionMode) {
                    toggleSelectionUI()
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated - 모드: ${if (isKnown) "아는 단어" else "일반"}, 챕터: $chapterTitle")

        setupUI()
        bindObservers()

        if (savedInstanceState == null) {
            loadInitialData()
        }
    }

    private fun loadInitialData() {
        Log.d(TAG, "초기 데이터 로드: PDF ID $pdfId, 챕터 $chapterTitle, 아는 단어 모드: $isKnown")
        // 기존 방식 그대로 유지
        viewModel.loadChapterWords(pdfId, chapterTitle, isKnown, sharedViewModel)
    }

    private fun setupUI() = with(binding) {
        // 타이틀 설정
        tvChapterTitle.text = chapterTitle
        // 화면 모드에 따른 UI 조정
        setupModeSpecificUI()
        // RecyclerView 설정
        setupRecyclerView()
        // 버튼 리스너 설정
        setupButtonListeners()
        // 초기 상태
        selectionModeContainer.visibility = View.GONE
    }

    /**
     * 모드에 따른 UI 조정 (아는 단어 또는 일반 모드)
     */
    private fun setupModeSpecificUI() = with(binding) {
        // 이동 버튼 텍스트 설정
        btnMoveToUnknown.text = if (isKnown) {
            getString(R.string.btn_move_to_normal)
        } else {
            getString(R.string.btn_move_to_known)
        }
    }

    /**
     * RecyclerView 설정
     */
    private fun setupRecyclerView() = with(binding.rvWords) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = wordAdapter
        setHasFixedSize(true)
    }

    /**
     * 버튼 리스너 설정
     */
    private fun setupButtonListeners() = with(binding) {
        // 이동 버튼 클릭 리스너
        btnMoveToUnknown.setOnClickListener { moveSelectedWords() }
        // 취소 버튼 클릭 리스너
        btnCancel.setOnClickListener { exitSelectionMode() }
        // 전체 선택 버튼 클릭 리스너
        btnSelectAll.setOnClickListener { wordAdapter.selectAll() }
        // 선택 해제 버튼 클릭 리스너
        btnClearSelection.setOnClickListener { wordAdapter.clearSelection() }
    }

    /**
     * 데이터 변경 및 이벤트 구독
     */
    private fun bindObservers() = with(viewLifecycleOwner) {
        // 뒤로가기 이벤트 - Quiz 스타일로 단순화
        repeatOnStarted {
            viewModel.back.collect {
                Log.d(TAG, "뒤로가기 이벤트")
                handleBackPressed()
            }
        }

        // 퀴즈 화면 이동 이벤트
        repeatOnStarted {
            viewModel.navigateToQuiz.collect { word ->
                navigateToQuiz(word)
            }
        }

        // 공유 ViewModel의 단어 상태 변경 이벤트 구독 (기존 방식 유지)
        repeatOnStarted {
            sharedViewModel.wordStatusChanged.collect { (changedPdfId, words) ->
                if (changedPdfId == pdfId && !isKnown) {
                    Log.d(TAG, "단어 상태 변경 이벤트 수신 - ${words.size}개 단어 상태 변경됨")
                    viewModel.refreshWordList()
                }
            }
        }

        // 챕터 제목 변경 이벤트
        viewModel.chapterTitle.observe(this) { title ->
            binding.tvChapterTitle.text = title
        }

        // 단어 목록 변경 이벤트
        viewModel.words.observe(this) { wordList ->
            updateWordList(wordList)
        }
    }

    /**
     * 뒤로가기 처리 - Quiz 스타일로 단순화
     */
    private fun handleBackPressed() {
        // 선택 모드인 경우 선택 모드만 종료
        if (wordAdapter.isInSelectionMode()) {
            Log.d(TAG, "선택 모드 종료")
            exitSelectionMode()
        } else {
            // 일반 모드인 경우 Fragment 뒤로가기
            Log.d(TAG, "Fragment 뒤로가기")
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * 단어 목록 UI 업데이트
     */
    private fun updateWordList(wordList: List<KanjiDetailModel>) {
        Log.d(TAG, "단어 목록 업데이트: ${wordList.size}개 단어")

        if (!isAdded || view == null) {
            Log.w(TAG, "단어 목록 업데이트 - Fragment not added to Activity")
            return
        }

        // 어댑터가 null이면 재설정
        if (binding.rvWords.adapter == null) {
            binding.rvWords.adapter = wordAdapter
        }

        // 단어 목록이 비어 있는 경우
        if (wordList.isEmpty()) {
            binding.tvEmptyMessage.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = if (isKnown) {
                getString(R.string.msg_no_known_words)
            } else {
                getString(R.string.msg_no_words)
            }
            binding.rvWords.visibility = View.GONE

            // 빈 목록으로 갱신
            wordAdapter.submitList(emptyList())
            return
        }

        // UI 스레드에서 안전하게 업데이트
        binding.root.post {
            try {
                wordAdapter.submitList(ArrayList(wordList))  // 새 리스트 인스턴스 생성

                binding.tvEmptyMessage.visibility = View.GONE
                binding.rvWords.visibility = View.VISIBLE

                Log.d(TAG, "단어 목록 UI 업데이트 완료: ${wordList.size}개 단어")
            } catch (e: Exception) {
                Log.e(TAG, "단어 목록 UI 업데이트 실패: ${e.message}", e)
            }
        }
    }

    /**
     * 퀴즈 화면으로 이동
     */
    private fun navigateToQuiz(word: KanjiDetailModel) {
        try {
            val allWords = ArrayList(wordAdapter.currentList)
            if (allWords.isEmpty()) {
                Log.d(TAG, "단일 단어만 퀴즈로 전달: ${word.kanji}")
                startActivity(Intent(requireContext(), QuizActivity::class.java).apply {
                    putExtra("word", word)
                })
                return
            }
            // 각 단어에 고유한 ID 설정
            val wordsWithIds = allWords.mapIndexed { index, kanjiDetail ->
                if (kanjiDetail.vocabularyBookOrder <= 0) {
                    kanjiDetail.copy(vocabularyBookOrder = index + 1)
                } else {
                    kanjiDetail
                }
            }

            val sortedWords = ArrayList(wordsWithIds.sortedBy { it.vocabularyBookOrder })

            val selectedWordWithId = wordsWithIds.find { it.kanji == word.kanji } ?: word.copy(
                vocabularyBookOrder = wordsWithIds.size + 1
            )

            // 퀴즈 액티비티 시작
            val intent = Intent(requireContext(), QuizActivity::class.java).apply {
                putExtra("word", selectedWordWithId)
                putParcelableArrayListExtra("words", sortedWords)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("퀴즈를 시작할 수 없습니다")
        }
    }

    private fun handleWordClick(word: KanjiDetailModel) {
        if (wordAdapter.isInSelectionMode()) return
        Log.d(TAG, "${if (isKnown) "아는" else "일반"} 단어 클릭: ${word.kanji} - 퀴즈로 이동")
        viewModel.onWordClick(word)
    }


    private fun exitSelectionMode() {
        wordAdapter.exitSelectionMode()
        toggleSelectionUI()
    }


    private fun toggleSelectionUI() {
        val isSelectionMode = wordAdapter.isInSelectionMode()
        Log.d(TAG, "선택 모드 UI 토글: $isSelectionMode")
        binding.selectionModeContainer.apply {
            visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            // 추가 애니메이션 적용 (옵션)
            alpha = if (isSelectionMode) 0f else 1f
            if (isSelectionMode) {
                visibility = View.VISIBLE
                animate().alpha(1f).setDuration(200).start()
            } else {
                animate().alpha(0f).setDuration(200).withEndAction {
                    visibility = View.GONE
                }.start()
            }
        }
    }

    private fun moveSelectedWords() {
        val selected = wordAdapter.getSelectedItems()
        if (selected.isEmpty()) {
            showToast(getString(R.string.msg_no_selected_words))
            return
        }
        try {
            if (isKnown) {
                Log.d(TAG, "${selected.size}개의 아는 단어를 일반 단어로 이동")
                sharedViewModel.removeKnownWords(pdfId, selected)
                removeWordsFromList(selected)
                showToast("${selected.size}개 단어가 일반 단어장으로 이동되었습니다")
            } else {
                Log.d(TAG, "${selected.size}개의 일반 단어를 아는 단어로 이동")
                sharedViewModel.addKnownWords(pdfId, selected)
                refreshVisibleWordList()
                showToast("${selected.size}개 단어가 아는 단어장으로 이동되었습니다")
            }
        } finally {
            // 공통 후처리
            exitSelectionMode()
        }
    }

    /**
     * 화면에 표시된 단어 목록 갱신
     */
    private fun refreshVisibleWordList() {
        // 전체 단어 목록 다시 로드 (뷰모델에서 숨김 처리된 단어 필터링)
        viewModel.loadChapterWords(pdfId, chapterTitle, isKnown, sharedViewModel)
    }

    /**
     * 목록에서 단어들 제거 (아는 단어 모드에서 사용)
     */
    private fun removeWordsFromList(wordsToRemove: List<KanjiDetailModel>) {
        if (isKnown) {
            // 아는 단어 화면에서는 실제로 제거
            val remainingWords = viewModel.words.value.orEmpty().filterNot { current ->
                wordsToRemove.any {
                    it.vocabularyBookOrder == current.vocabularyBookOrder && it.kanji == current.kanji
                }
            }
            viewModel.updateWordList(remainingWords)
        } else {
            // 일반 화면에서는 숨김 처리 후 새로고침
            viewModel.refreshWordList()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - 모드: ${if (isKnown) "아는 단어" else "일반"}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView - 리소스 정리")
    }
}