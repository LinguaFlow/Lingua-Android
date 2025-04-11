package com.yju.presentation.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class AppApplication :Application(){
    override fun onCreate() {
        super.onCreate()
//        Timber.plant(Timber.DebugTree())
    }
}