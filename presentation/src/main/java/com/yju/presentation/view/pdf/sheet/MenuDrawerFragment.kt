package com.yju.presentation.view.pdf.sheet

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.yju.presentation.R
import com.yju.presentation.databinding.FragmentMenuDrawerBinding
import com.yju.presentation.ext.repeatOnStarted
import com.yju.presentation.view.home.HomeViewModel
import com.yju.presentation.view.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MenuDrawerFragment : DialogFragment() {

    private var _binding: FragmentMenuDrawerBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val viewModel: MenuDrawerViewModel by viewModels()

    companion object {
        private const val TAG = "MenuDrawerFragment"
        fun newInstance() = MenuDrawerFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_menu_drawer, container, false)
        binding.vm = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
        setupSystemUIInsets()
        setupUserInfo()
        setupMenuClickListeners()
        observeViewModel()
    }

    private fun setupDialog() {
        dialog?.window?.let { window ->
            // Edge-to-Edge 설정
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // 윈도우 인셋 컨트롤러 설정
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.isAppearanceLightStatusBars = true
            windowInsetsController.isAppearanceLightNavigationBars = true

            // 레이아웃 설정
            window.setLayout(
                resources.getDimensionPixelSize(R.dimen.drawer_width),
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            window.setGravity(Gravity.END)
            window.setWindowAnimations(R.style.DrawerAnimation)
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }

        dialog?.setCanceledOnTouchOutside(true)
    }

    private fun setupSystemUIInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // 상단 패딩 (상태바)
            binding.headerSection.updatePadding(top = systemBars.top + 24)

            // 하단 패딩 (네비게이션 바)
            binding.bottomSection.updatePadding(bottom = systemBars.bottom + 24)

            insets
        }
    }

    private fun setupUserInfo() {
        val (userName, userEmail) = viewModel.getUserInfo()
        binding.tvUserName.text = userName
        binding.tvUserEmail.text = userEmail
        binding.ivProfileImage.setImageResource(R.drawable.ic_person)
    }

    private fun setupMenuClickListeners() {
        with(binding) {
            menuPdfManage.setOnClickListener {
                Log.d(TAG, "PDF 관리 메뉴 클릭")
                viewModel.onPdfManagerClick()
            }

            menuVocabulary.setOnClickListener {
                Log.d(TAG, "단어장 메뉴 클릭")
                viewModel.onVocabularyClick()
            }

            menuSettings.setOnClickListener {
                Log.d(TAG, "설정 메뉴 클릭")
                viewModel.onSettingsClick()
            }

            menuLogout.setOnClickListener {
                Log.d(TAG, "로그아웃 메뉴 클릭")
                viewModel.performLogout()
            }

            headerSection.setOnClickListener {
                Log.d(TAG, "프로필 헤더 클릭")
                showToast("프로필 편집 기능은 준비 중입니다")
            }
        }
    }

    private fun observeViewModel() {
        repeatOnStarted {
            viewModel.navigateToPdfManager.collect {
                homeViewModel.navigateToPdfUpload()
                dismiss()
            }
        }

        repeatOnStarted {
            viewModel.navigateToVocabulary.collect {
                showToast("단어장 기능은 준비 중입니다")
                dismiss()
            }
        }

        repeatOnStarted {
            viewModel.navigateToSettings.collect {
                showToast("설정 기능은 준비 중입니다")
                dismiss()
            }
        }

        repeatOnStarted {
            viewModel.navigateToLogin.collect {
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        Log.d(TAG, "로그인 화면으로 이동")
        dismiss()

        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun getTheme(): Int = R.style.DrawerDialogTheme

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}