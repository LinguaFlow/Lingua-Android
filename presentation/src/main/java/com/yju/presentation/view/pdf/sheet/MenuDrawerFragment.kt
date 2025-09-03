package com.yju.presentation.view.pdf.sheet

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
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

        fun newInstance(): MenuDrawerFragment {
            return MenuDrawerFragment()
        }
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
        setupUserInfo()
        setupMenuClickListeners()
        observeViewModel()
    }

    private fun setupDialog() {
        dialog?.window?.let { window ->
            window.setLayout(
                resources.getDimensionPixelSize(R.dimen.drawer_width),
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            window.setGravity(Gravity.END)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            window.setWindowAnimations(R.style.DrawerAnimation)
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }

        // 배경 클릭 시 dismiss 처리
        dialog?.setCanceledOnTouchOutside(true)
        dialog?.setOnCancelListener {
            // 자연스러운 애니메이션으로 사라지도록
            dismissWithAnimation()
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
        // PDF 관리로 이동
        repeatOnStarted {
            viewModel.navigateToPdfManager.collect {
                homeViewModel.navigateToPdfUpload()
                dismissWithAnimation()
            }
        }

        // 단어장 기능 (준비중)
        repeatOnStarted {
            viewModel.navigateToVocabulary.collect {
                showToast("단어장 기능은 준비 중입니다")
                dismissWithAnimation()
            }
        }

        // 설정 기능 (준비중)
        repeatOnStarted {
            viewModel.navigateToSettings.collect {
                showToast("설정 기능은 준비 중입니다")
                dismissWithAnimation()
            }
        }

        // 로그인 화면으로 이동
        repeatOnStarted {
            viewModel.navigateToLogin.collect {
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        Log.d(TAG, "로그인 화면으로 이동")
        dismissWithAnimation() // 애니메이션과 함께 닫기

        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun dismissWithAnimation() {
        // 애니메이션이 적용된 dismiss
        dialog?.window?.setWindowAnimations(R.style.DrawerAnimation)
        dismiss()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun getTheme(): Int {
        return R.style.DrawerDialogTheme  // 커스텀 테마 사용
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}