package com.yju.presentation.base

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yju.presentation.BR
import com.yju.presentation.R
import kotlinx.coroutines.launch
import java.lang.reflect.ParameterizedType

abstract class BaseActivity<B : ViewDataBinding, VM : BaseViewModel>(
    @LayoutRes private val layoutId: Int
) : AppCompatActivity() {

    protected lateinit var binding: B

    private val viewModelClass = ((javaClass.genericSuperclass as? ParameterizedType)
        ?.actualTypeArguments
        ?.getOrNull(1) as? Class<VM>)?.kotlin
        ?: throw IllegalArgumentException("Invalid ViewModel class")

    protected open val viewModel by ViewModelLazy(
        viewModelClass,
        { viewModelStore },
        { defaultViewModelProviderFactory },
        { defaultViewModelCreationExtras }
    )

    // 로딩 애니메이션 설정
    private val animationDelay = 200L
    private val animationDuration = 600L * 2

    // 로딩 다이얼로그 추가 - lazy로 초기화
    private val loadingDialog by lazy {
        AppCompatDialog(this).apply {
            setContentView(R.layout.item_progress_loading)
            setCancelable(false)
            window?.apply {
                setDimAmount(0.7f)
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            }
        }
    }

    // 로딩 애니메이터 캐싱
    private var animators: Triple<ObjectAnimator?, ObjectAnimator?, ObjectAnimator?> = Triple(null, null, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUi()
        setupObserve()
    }

    override fun onStart() {
        super.onStart()
        setupKeyboardAutoHide(binding.root)  // 키보드 자동 숨김 설정 추가
    }

    private fun setupObserve() {
        // UI 이벤트 (로딩, 토스트 등) 감지하여 처리
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEventFlow.collect { event ->
                    handleUiEvent(event)
                }
            }
        }
    }

    /**
     * UI 이벤트 처리 - 하위 클래스에서 필요시 오버라이드 가능
     */
    protected open fun handleUiEvent(event: BaseViewModel.UiEvent) {
        when (event) {
            is BaseViewModel.UiEvent.Loading.Show -> showLoadingDialog()
            is BaseViewModel.UiEvent.Loading.Hide -> dismissLoadingDialog()
            is BaseViewModel.UiEvent.Toast.Normal -> showToast(event.message)
            is BaseViewModel.UiEvent.Toast.NormalRes -> showToast(getString(event.messageResId))
            is BaseViewModel.UiEvent.Toast.Success -> showSuccessToast(event.message)
            is BaseViewModel.UiEvent.Toast.SuccessRes -> showSuccessToast(getString(event.messageResId))
        }
    }

    /**
     * 로딩 다이얼로그 표시
     */
    fun showLoadingDialog() {
        try {
            // 이미 로딩 중이면 중복 표시 방지
            if (loadingDialog.isShowing) return

            val circle1 = loadingDialog.findViewById<View>(R.id.circle1)
            val circle2 = loadingDialog.findViewById<View>(R.id.circle2)
            val circle3 = loadingDialog.findViewById<View>(R.id.circle3)

            // 애니메이터가 없으면 새로 생성
            if (animators.first == null) {
                val animator1 = ObjectAnimator.ofFloat(circle1, "translationY", 0f, -50f, 0f).apply {
                    duration = animationDuration
                    interpolator = AccelerateDecelerateInterpolator()
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    startDelay = animationDelay * 0
                }

                val animator2 = ObjectAnimator.ofFloat(circle2, "translationY", 0f, -50f, 0f).apply {
                    duration = animationDuration
                    interpolator = AccelerateDecelerateInterpolator()
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    startDelay = animationDelay * 1
                }

                val animator3 = ObjectAnimator.ofFloat(circle3, "translationY", 0f, -50f, 0f).apply {
                    duration = animationDuration
                    interpolator = AccelerateDecelerateInterpolator()
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    startDelay = animationDelay * 2
                }

                animators = Triple(animator1, animator2, animator3)
            }

            // 애니메이션 시작
            animators.first?.start()
            animators.second?.start()
            animators.third?.start()

            // 다이얼로그 표시
            loadingDialog.show()
        } catch (e: Exception) {
            Log.e("BaseActivity", "로딩 다이얼로그 표시 오류: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 로딩 다이얼로그 숨김
     */
    fun dismissLoadingDialog() {
        try {
            // 애니메이션 중지
            animators.first?.cancel()
            animators.second?.cancel()
            animators.third?.cancel()

            // 다이얼로그가 표시 중인 경우에만 닫기
            if (loadingDialog.isShowing) {
                loadingDialog.dismiss()
            }
        } catch (e: Exception) {
            Log.e("BaseActivity", "로딩 다이얼로그 숨김 오류: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 간편 로딩 표시 메서드 - 외부 호출용
     */
    open fun showLoading() {
        showLoadingDialog()
    }

    /**
     * 간편 로딩 숨김 메서드 - 외부 호출용
     */
    open fun hideLoading() {
        dismissLoadingDialog()
    }

    /**
     * 일반 토스트 표시
     */
    open fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 성공 토스트 표시
     */
    protected open fun showSuccessToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * UI 초기화
     */
    private fun setupUi() {
        binding = DataBindingUtil.setContentView(this, layoutId)
        Log.d("BindingCheck", "Binding class: ${binding.javaClass.name}")

        with(binding) {
            setVariable(BR.vm, viewModel)
            lifecycleOwner = this@BaseActivity
        }
    }

    /**
     * 액티비티 종료 시 리소스 해제
     */
    override fun onDestroy() {
        // 애니메이션 정리
        animators.first?.cancel()
        animators.second?.cancel()
        animators.third?.cancel()
        animators = Triple(null, null, null)

        // 다이얼로그가 표시 중이면 닫기
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }

        super.onDestroy()
    }

    // 키보드 숨기기 함수
    fun hideKeyboard() {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
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