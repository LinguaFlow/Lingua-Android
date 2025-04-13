package com.yju.domain.util

sealed class DatabaseState<out T> {
    data class Success<T>(val data:T) : DatabaseState<T>()
    data class Error(val exception: Exception , val message :String? = null ) : DatabaseState<Nothing>()
}