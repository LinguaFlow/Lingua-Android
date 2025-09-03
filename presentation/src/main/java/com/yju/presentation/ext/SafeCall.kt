package com.yju.presentation.ext

inline fun <T> safeCall(block: () -> T): Result<T> =
    runCatching(block)
