package com.yju.presentation.di

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.yju.presentation.BuildConfig
import dagger.hilt.android.HiltAndroidApp


import timber.log.Timber


@HiltAndroidApp  // 이 어노테이션 추가
class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Kakao SDK 초기화
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        Timber.d("Application initialized")
    }
}