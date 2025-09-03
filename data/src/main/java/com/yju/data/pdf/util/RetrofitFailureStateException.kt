package com.yju.data.pdf.util

class RetrofitFailureStateException(error: String ?, val code: Int) : Exception(error)