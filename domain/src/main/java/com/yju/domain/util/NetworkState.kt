package com.yju.domain.util

import java.io.IOException

sealed class NetworkState<out T : Any> {
    data class Success<T : Any>(val body: T) : NetworkState<T>()
    data class Failure(val code: Int, val message: String?) : NetworkState<Nothing>()
    data class NetworkError(val error: IOException) : NetworkState<Nothing>()
    data class UnknownError(val throwable: Throwable?, val errorState: String) : NetworkState<Nothing>()
}