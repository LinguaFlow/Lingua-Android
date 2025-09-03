package com.yju.presentation.di

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yju.domain.kanji.usecase.GetAllKanjiVocabularyUseCase
import com.yju.domain.known.usecase.DeleteKnownWordKanjiByBookNameUseCase
import com.yju.presentation.view.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LauncherActivity : AppCompatActivity() {

    @Inject lateinit var getAllKanji: GetAllKanjiVocabularyUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val hasData = runCatching { getAllKanji() }
                .getOrElse { emptyList() }
                .isNotEmpty()

            val intent = Intent(this@LauncherActivity, HomeActivity::class.java).apply {

                putExtra("START_WITH_VIEWER", hasData)
            }
            startActivity(intent)
            finish() // 자기 자신 종료
        }
    }
}