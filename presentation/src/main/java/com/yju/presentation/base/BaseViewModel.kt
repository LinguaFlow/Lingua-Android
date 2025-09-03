package com.yju.presentation.base

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yju.presentation.util.MutableEventFlow
import com.yju.presentation.util.asEventFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val _uiEventFlow = MutableEventFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asEventFlow()

    fun baseEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEventFlow.emit(event)
        }
    }

    sealed class UiEvent {
        sealed class Toast : UiEvent() {
            data class Normal(val message: String) : Toast()
            data class NormalRes(@StringRes val messageResId: Int) : Toast()
            data class Success(val message: String) : Toast()
            data class SuccessRes(@StringRes val messageResId: Int) : Toast()
        }

        sealed class Loading : UiEvent() {
            object Show : Loading()
            object Hide : Loading()
        }
    }
}