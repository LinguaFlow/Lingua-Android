package com.yju.domain.util

sealed class NetworkStat<out T : Any> {
    data class Success<T : Any>(val body: T) : NetworkStat<T>()
}