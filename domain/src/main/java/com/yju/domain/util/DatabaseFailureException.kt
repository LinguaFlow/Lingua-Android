package com.yju.domain.util

class DatabaseFailureException(override val message: String, val code: Int = -1) : Exception(message)
