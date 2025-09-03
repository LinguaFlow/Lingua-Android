package com.yju.presentation.module

import android.content.Context

import com.yju.presentation.view.speech.TTSHelper
import com.yju.presentation.view.speech.TTSManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TTSModule {

    @Module
    @InstallIn(SingletonComponent::class)
    object SpeechModule {

        @Provides
        @Singleton
        fun provideTTSHelper(@ApplicationContext context: Context): TTSHelper {
            return TTSHelper(context)
        }

        @Provides
        @Singleton
        fun provideTTSManager(ttsHelper: TTSHelper): TTSManager {
            return TTSManager(ttsHelper)
        }
    }
}