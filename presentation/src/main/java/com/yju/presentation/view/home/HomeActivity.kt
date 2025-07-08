package com.yju.presentation.view.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.presentation.R
import com.yju.presentation.base.BaseActivity
import com.yju.presentation.databinding.ActivityHomeBinding
import com.yju.presentation.util.FragmentTransitionManager
import com.yju.presentation.util.Navigation
import com.yju.presentation.view.pdf.chapter.PdfChapterFragment
import com.yju.presentation.view.pdf.word.PdfWordFragment
import com.yju.presentation.view.pdf.upload.PdfViewerFragment
import com.yju.presentation.view.quiz.QuizActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity :
    BaseActivity<ActivityHomeBinding, HomeViewModel>(R.layout.activity_home) {

    private val TAG = "HomeActivity"
    private lateinit var transitionManager: FragmentTransitionManager
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleBackPress()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel
        transitionManager = FragmentTransitionManager(supportFragmentManager)
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        if (savedInstanceState == null) {
            showViewerFragment()
        }

        setupViewModelObservers()
    }

    private fun setupViewModelObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { event ->
                    Log.d(TAG, "Navigation 이벤트 수신: $event")
                    handleNavigationEvent(event)
                }
            }
        }
    }


    private fun handleNavigationEvent(event: Navigation) {
        when (event) {
            is Navigation.ToPdfViewer -> showViewerFragment(event.bookId)
            is Navigation.ToPdfChapter -> showChapterFragment(event.pdfId)
            is Navigation.ToPdfWords -> {
                Log.d(TAG, "Word 화면으로 이동: ${event.pdfId}, ${event.chapterTitle}")
                showWordFragment(event.pdfId, event.chapterTitle)
            }
        }
    }

    private fun handleBackPress() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        when (currentFragment) {
            is PdfWordFragment -> {
                // 단어 → 챕터
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                }
            }
            is PdfChapterFragment -> {
                // 챕터 → 뷰어
                supportFragmentManager.popBackStackImmediate(
                    null,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                showViewerFragment()
            }
            is PdfViewerFragment -> {
                // 뷰어 → 앱 종료
                finish()
            }
            else -> finish()
        }
    }

    fun showViewerFragment(bookId: String? = null) {
        val fragment = PdfViewerFragment().apply {
            bookId?.let {
                arguments = Bundle().apply { putString("book_id", it) }
            }
        }

        transitionManager.navigateWithAnimation(
            R.id.fragmentContainer,
            fragment,
            false,
            null
        )
    }


    fun showChapterFragment(pdfId: Long) {
        val fragment = PdfChapterFragment.newInstance(pdfId)

        transitionManager.navigateWithAnimation(
            R.id.fragmentContainer,
            fragment,
            true,
            "PdfChapter_$pdfId"
        )
    }

    internal fun showWordFragment(pdfId: Long, chapterTitle: String) {
        val fragment = PdfWordFragment.newInstance(pdfId, chapterTitle)

        transitionManager.navigateWithAnimation(
            R.id.fragmentContainer,
            fragment,
            true,
            "PdfWord_${pdfId}_$chapterTitle"
        )
    }

    override fun onDestroy() {
        backPressedCallback.remove()
        super.onDestroy()
    }
}