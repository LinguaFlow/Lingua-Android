package com.yju.presentation.view.home

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yju.presentation.R
import com.yju.presentation.base.BaseActivity
import com.yju.presentation.databinding.ActivityHomeBinding
import com.yju.presentation.util.FragmentTransitionManager
import com.yju.presentation.util.Navigation
import com.yju.presentation.view.pdf.chapter.PdfChapterFragment
import com.yju.presentation.view.pdf.word.PdfWordFragment
import com.yju.presentation.view.pdf.upload.PdfViewerFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : BaseActivity<ActivityHomeBinding, HomeViewModel>(R.layout.activity_home) {

    private lateinit var transitionManager: FragmentTransitionManager
    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleBackPress()
        }
    }

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel

        setupWindowInsets()
        setupManagers()
        setupViewModelObservers()

        if (savedInstanceState == null) {
            showViewerFragment()
        }
    }

    private fun setupWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.fragmentContainer.updatePadding(
                bottom = systemBars.bottom
            )
            insets
        }
    }

    private fun setupManagers() {
        transitionManager = FragmentTransitionManager(supportFragmentManager)
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
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
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                }
            }
            is PdfChapterFragment -> {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.animator.slide_in_left,
                        R.animator.slide_out_right
                    )
                    .replace(R.id.fragmentContainer, PdfViewerFragment())
                    .commit()
            }
            is PdfViewerFragment -> finish()
            else -> finish()
        }
    }

    fun showViewerFragment(bookId: String? = null) {
        val fragment = PdfViewerFragment().apply {
            bookId?.let {
                arguments = Bundle().apply { putString("book_id", it) }
            }
        }

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        val withAnimation = currentFragment is PdfChapterFragment

        transitionManager.navigateWithAnimation(
            R.id.fragmentContainer,
            fragment,
            false,
            null,
            withAnimation = withAnimation
        )
    }

    fun showChapterFragment(pdfId: Long) {
        val fragment = PdfChapterFragment.newInstance(pdfId)
        transitionManager.navigateWithAnimation(
            R.id.fragmentContainer,
            fragment,
            true,
            "PdfChapter_$pdfId",
            withAnimation = true
        )
    }

    internal fun showWordFragment(pdfId: Long, chapterTitle: String) {
        val fragment = PdfWordFragment.newInstance(pdfId, chapterTitle)
        transitionManager.navigateWithAnimation(
            R.id.fragmentContainer,
            fragment,
            true,
            "PdfWord_${pdfId}_${chapterTitle.replace(":", "_")}",
            withAnimation = true
        )
    }

    override fun onDestroy() {
        backPressedCallback.remove()
        super.onDestroy()
    }
}