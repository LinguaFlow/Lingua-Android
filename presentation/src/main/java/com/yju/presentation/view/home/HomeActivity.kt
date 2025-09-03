package com.yju.presentation.view.home

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.core.view.GravityCompat
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
import com.yju.presentation.view.pdf.sheet.MenuDrawerFragment
import com.yju.domain.auth.util.SharedPreferenceUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity :
    BaseActivity<ActivityHomeBinding, HomeViewModel>(R.layout.activity_home) {

    @Inject
    lateinit var prefs: SharedPreferenceUtil

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

        setupNavigationDrawer()

        if (savedInstanceState == null) {
            showViewerFragment()
        }

        setupViewModelObservers()
    }

    private fun setupNavigationDrawer() {
        setupUserInfo()
        setupDrawerMenuClickListeners()
    }

    private fun setupUserInfo() {
        val userName = prefs.getString("userName", "사용자")
        val userEmail = prefs.getString("userEmail", "user@example.com")

        binding.tvUserName.text = userName
        binding.tvUserEmail.text = userEmail
    }

    private fun setupDrawerMenuClickListeners() {
        with(binding) {
            // PDF 관리 메뉴
            menuPdfManage.setOnClickListener {
                Log.d(TAG, "PDF 관리 메뉴 클릭")
                closeDrawer()
                showViewerFragment()
            }

            // 단어장 메뉴
            menuVocabulary.setOnClickListener {
                Log.d(TAG, "단어장 메뉴 클릭")
                closeDrawer()
                showToast("단어장 기능은 준비 중입니다")
            }

            // 설정 메뉴
            menuSettings.setOnClickListener {
                Log.d(TAG, "설정 메뉴 클릭")
                closeDrawer()
                showToast("설정 기능은 준비 중입니다")
            }

            // 로그아웃 메뉴 - MenuDrawerFragment 사용
            menuLogout.setOnClickListener {
                Log.d(TAG, "로그아웃 메뉴 클릭 - MenuDrawerFragment 호출")
                closeDrawer()
                showMenuDrawer()
            }

            // 헤더 클릭 시 프로필 편집
            headerSection.setOnClickListener {
                Log.d(TAG, "헤더 섹션 클릭")
                closeDrawer()
                showToast("프로필 편집 기능은 준비 중입니다")
            }
        }
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
        when {
            isDrawerOpen() -> closeDrawer()
            else -> {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

                when (currentFragment) {
                    is PdfWordFragment -> {
                        // 단어 → 챕터 (슬라이드 애니메이션)
                        if (supportFragmentManager.backStackEntryCount > 0) {
                            // 애니메이션과 함께 뒤로가기
                            supportFragmentManager.beginTransaction()
                                .setCustomAnimations(
                                    R.animator.slide_in_left,
                                    R.animator.slide_out_right
                                )
                                .commit()
                            supportFragmentManager.popBackStack()
                        }
                    }
                    is PdfChapterFragment -> {
                        // 챕터 → 뷰어 (슬라이드 애니메이션)
                        // Fragment 재생성 방지: 기존 PdfViewerFragment 재사용
                        supportFragmentManager.beginTransaction()
                            .setCustomAnimations(
                                R.animator.slide_in_left,
                                R.animator.slide_out_right
                            )
                            .replace(R.id.fragmentContainer, PdfViewerFragment())
                            .commit()
                    }
                    is PdfViewerFragment -> {
                        // 뷰어 → 앱 종료
                        finish()
                    }
                    else -> finish()
                }
            }
        }
    }

    // MenuDrawerFragment 표시
    private fun showMenuDrawer() {
        val menuFragment = MenuDrawerFragment.newInstance()
        menuFragment.show(supportFragmentManager, "MenuDrawer")
    }

    fun showViewerFragment(bookId: String? = null) {
        val fragment = PdfViewerFragment().apply {
            bookId?.let {
                arguments = Bundle().apply { putString("book_id", it) }
            }
        }

        // PdfChapter에서 돌아올 때는 왼쪽 슬라이드 애니메이션
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

        // Viewer에서 Chapter로 갈 때는 오른쪽 슬라이드 애니메이션
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

        // Chapter에서 Word로 갈 때는 오른쪽 슬라이드 애니메이션
        transitionManager.navigateWithAnimation(
            R.id.fragmentContainer,
            fragment,
            true,
            "PdfWord_${pdfId}_${chapterTitle.replace(":", "_")}", // 콜론을 언더스코어로 치환
            withAnimation = true
        )
    }

    // 드로어 관련 메서드들
    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    fun closeDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    fun isDrawerOpen(): Boolean {
        return binding.drawerLayout.isDrawerOpen(GravityCompat.START)
    }

    fun toggleDrawer() {
        if (isDrawerOpen()) {
            closeDrawer()
        } else {
            openDrawer()
        }
    }

    override fun onDestroy() {
        backPressedCallback.remove()
        super.onDestroy()
    }
}