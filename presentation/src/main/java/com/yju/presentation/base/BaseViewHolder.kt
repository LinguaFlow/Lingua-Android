package com.yju.presentation.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView

open class BaseViewHolder(
    @LayoutRes layoutResId: Int,
    parent: ViewGroup?,
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent?.context)
        .inflate(layoutResId, parent, false)
), LifecycleOwner {
    // 프로퍼티 이름을 변경하여 JVM 시그니처 충돌 방지
    private val viewBinding: ViewDataBinding = DataBindingUtil.bind(itemView)!!

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }

    // 제네릭 메소드는 이름을 그대로 유지
    fun <T : ViewDataBinding> getBinding(): T {
        @Suppress("UNCHECKED_CAST")
        return viewBinding as T
    }

    fun onBind(item: Any?, eventHolder: Any?) {
        try {
            viewBinding.setVariable(com.yju.presentation.BR.vm, item)
            viewBinding.setVariable(com.yju.presentation.BR.eventHolder, eventHolder)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        viewBinding.executePendingBindings()
    }

    fun onAttach() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDetach() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}