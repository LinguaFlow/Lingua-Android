package com.yju.presentation.base

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yju.presentation.BR
import kotlinx.coroutines.launch
import java.lang.reflect.ParameterizedType

abstract class BaseActivity<B : ViewDataBinding, VM : BaseViewModel>(
    @LayoutRes private val layoutId: Int
) : AppCompatActivity() {

    protected lateinit var binding: B

    private val viewModelClass = (
            (javaClass.genericSuperclass as ParameterizedType?)
                ?.actualTypeArguments
                ?.get(1) as Class<VM>
            ).kotlin

    protected open val viewModel by ViewModelLazy(
        viewModelClass,
        { viewModelStore },
        { defaultViewModelProviderFactory },
        { defaultViewModelCreationExtras }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUi()
        setupObserve()
    }

    private fun setupObserve() {
        // UI 이벤트 (로딩, 토스트 등) 감지
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEventFlow.collect { event ->
                    handleUiEvent(event)
                }
            }
        }
    }

    /**
     * UI 이벤트 처리 메서드
     * 각 Activity에서 오버라이드하여 추가 이벤트 처리 가능
     */
    protected open fun handleUiEvent(event: BaseViewModel.UiEvent) {
        when (event) {
            is BaseViewModel.UiEvent.Loading.Show -> {
                showLoading()
            }
            is BaseViewModel.UiEvent.Loading.Hide -> {
                hideLoading()
            }
            is BaseViewModel.UiEvent.Toast.Normal -> {
                showToast(event.message)
            }
            is BaseViewModel.UiEvent.Toast.Success -> {
                showSuccessToast(event.message)
            }
            else -> {
                // 하위 클래스에서 처리할 수 있도록 비워둠
            }
        }
    }

    /**
     * 로딩 표시 메서드
     * 각 Activity에서 오버라이드하여 구현
     */
    protected open fun showLoading() {
        // 기본 구현은 비어있음, 하위 클래스에서 오버라이드하여 구현
    }

    /**
     * 로딩 숨김 메서드
     * 각 Activity에서 오버라이드하여 구현
     */
    protected open fun hideLoading() {
        // 기본 구현은 비어있음, 하위 클래스에서 오버라이드하여 구현
    }

    /**
     * 일반 토스트 표시 메서드
     */
    protected open fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 성공 토스트 표시 메서드
     * 필요에 따라 하위 클래스에서 오버라이드하여 구현
     */
    protected open fun showSuccessToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupUi() {
        binding = DataBindingUtil.setContentView(this, layoutId)
        Log.d("BindingCheck", "Binding class: ${binding.javaClass.name}, Binding object: $binding")
        with(binding) {
            setVariable(BR.vm, viewModel)
            lifecycleOwner = this@BaseActivity
        }
    }
}