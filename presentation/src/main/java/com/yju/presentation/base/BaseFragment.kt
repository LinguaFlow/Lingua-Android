package com.yju.presentation.base

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yju.presentation.BR
import kotlinx.coroutines.launch
import java.lang.reflect.ParameterizedType

abstract class BaseFragment<B : ViewDataBinding, VM : BaseViewModel>(
    @LayoutRes private val layoutId: Int
) : Fragment() {

    private var _binding: B? = null
    protected val binding: B
        get() = _binding!!

    protected open val applyTransition: Boolean = true

    private val viewModelClass = ((javaClass.genericSuperclass as? ParameterizedType)
        ?.actualTypeArguments
        ?.getOrNull(1) as? Class<VM>)?.kotlin
        ?: throw IllegalArgumentException("Invalid ViewModel class")

    // 수정된 부분: ViewModelLazy 대신 지연 초기화로 변경하고 ViewModelProvider 직접 사용
    protected open val viewModel by ViewModelLazy(
        viewModelClass,
        { viewModelStore },
        { defaultViewModelProviderFactory },
        { defaultViewModelCreationExtras },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.setVariable(BR.vm, viewModel)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUiEventObserver()
        setupKeyboardAutoHide(view)  // 키보드 자동 숨김 설정 추가
    }

    // UI 이벤트 관찰 설정
    private fun setupUiEventObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEventFlow.collect { event ->
                    handleUiEvent(event)
                }
            }
        }
    }

    // UI 이벤트 처리 - 서브클래스에서 오버라이드 가능
    protected open fun handleUiEvent(event: BaseViewModel.UiEvent) {
        when (event) {
            is BaseViewModel.UiEvent.Loading.Show -> showLoading()
            is BaseViewModel.UiEvent.Loading.Hide -> hideLoading()
            is BaseViewModel.UiEvent.Toast.Normal -> showToast(event.message)
            is BaseViewModel.UiEvent.Toast.NormalRes -> showToast(getString(event.messageResId))
            is BaseViewModel.UiEvent.Toast.Success -> showSuccessToast(event.message)
            is BaseViewModel.UiEvent.Toast.SuccessRes -> showSuccessToast(getString(event.messageResId))
        }
    }

    // 로딩 표시 - 액티비티의 중앙화된 로딩 사용
    protected open fun showLoading() {
        (requireActivity() as? BaseActivity<*, *>)?.showLoading()
    }

    // 로딩 숨김 - 액티비티의 중앙화된 로딩 사용
    protected open fun hideLoading() {
        (requireActivity() as? BaseActivity<*, *>)?.hideLoading()
    }

    // 토스트 표시
    protected open fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // 성공 토스트 표시
    protected open fun showSuccessToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // 키보드 숨기기 함수
    fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        requireActivity().currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // 키보드 자동 숨김 설정 함수
    fun setupKeyboardAutoHide(view: View) {
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                hideKeyboard()
                false
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupKeyboardAutoHide(innerView)
            }
        }
    }
}