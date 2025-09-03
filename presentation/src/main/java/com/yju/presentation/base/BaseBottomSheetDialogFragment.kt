package com.yju.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.lang.reflect.ParameterizedType

abstract class BaseBottomSheetDialogFragment<B : ViewDataBinding, VM : BaseViewModel>(
    @LayoutRes private val layoutId: Int
) : BottomSheetDialogFragment() {

    private var _binding: B? = null
    protected val binding: B
        get() = _binding!!

    private val viewModelClass = ((javaClass.genericSuperclass as? ParameterizedType)
        ?.actualTypeArguments
        ?.getOrNull(1) as? Class<VM>)?.kotlin
        ?: throw IllegalArgumentException("Invalid ViewModel class")

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

    // UI 이벤트 처리
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

    // 로딩 표시
    protected open fun showLoading() {
        (requireActivity() as? BaseActivity<*, *>)?.showLoading()
    }

    // 로딩 숨김
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}