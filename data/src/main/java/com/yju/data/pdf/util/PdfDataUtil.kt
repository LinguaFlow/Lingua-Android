package com.yju.data.pdf.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yju.domain.kanji.model.KanjiDetailModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PdfDataUtil @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val TAG = "PdfDataUtil"
    }

    // PDF별로 분리된 SharedPreferences 생성
    private fun getPdfPrefs(pdfId: Long): SharedPreferences {
        return context.getSharedPreferences("pdf_data_$pdfId", Context.MODE_PRIVATE)
    }

    // ===== 모르는 단어 관리 =====
    fun saveKnownWords(pdfId: Long, words: List<KanjiDetailModel>) {
        val prefs = getPdfPrefs(pdfId)
        val gson = Gson()
        val json = gson.toJson(words)
        prefs.edit { putString("known_words", json) }
        Log.d(TAG, "모르는 단어 저장: PDF $pdfId, ${words.size}개")
    }

    fun getKnownWords(pdfId: Long): List<KanjiDetailModel> {
        val prefs = getPdfPrefs(pdfId)
        val json = prefs.getString("known_words", "[]") ?: "[]"
        return try {
            val gson = Gson()
            val type = object : TypeToken<List<KanjiDetailModel>>() {}.type
            gson.fromJson<List<KanjiDetailModel>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "모르는 단어 로드 실패: ${e.message}")
            emptyList()
        }
    }

    // ===== 숨김 단어 관리 =====
    fun saveHiddenWords(pdfId: Long, hiddenWords: Set<String>) {
        val prefs = getPdfPrefs(pdfId)
        prefs.edit { putStringSet("hidden_words", hiddenWords) }
        Log.d(TAG, "숨김 단어 저장: PDF $pdfId, ${hiddenWords.size}개")
    }

    fun getHiddenWords(pdfId: Long): Set<String> {
        val prefs = getPdfPrefs(pdfId)
        return prefs.getStringSet("hidden_words", emptySet()) ?: emptySet()
    }

    // ===== 챕터 관리 =====
    fun saveKnownChapters(pdfId: Long, chapters: List<String>) {
        val prefs = getPdfPrefs(pdfId)
        val gson = Gson()
        val json = gson.toJson(chapters)
        prefs.edit { putString("known_chapters", json) }
        Log.d(TAG, "챕터 저장: PDF $pdfId, ${chapters.size}개")
    }

    fun getKnownChapters(pdfId: Long): List<String> {
        val prefs = getPdfPrefs(pdfId)
        val json = prefs.getString("known_chapters", "[]") ?: "[]"
        return try {
            val gson = Gson()
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "챕터 로드 실패: ${e.message}")
            emptyList()
        }
    }

    // ===== 설정 관리 =====
    fun setAutoLoadDisabled(pdfId: Long, disabled: Boolean) {
        val prefs = getPdfPrefs(pdfId)
        prefs.edit { putBoolean("autoload_disabled", disabled) }
    }

    fun isAutoLoadDisabled(pdfId: Long): Boolean {
        val prefs = getPdfPrefs(pdfId)
        return prefs.getBoolean("autoload_disabled", false)
    }

    fun setKnownWordsLoaded(pdfId: Long, loaded: Boolean) {
        val prefs = getPdfPrefs(pdfId)
        prefs.edit { putBoolean("known_words_loaded", loaded) }
    }

    fun isKnownWordsLoaded(pdfId: Long): Boolean {
        val prefs = getPdfPrefs(pdfId)
        return prefs.getBoolean("known_words_loaded", false)
    }

    fun setHiddenStatusInitialized(pdfId: Long, initialized: Boolean) {
        val prefs = getPdfPrefs(pdfId)
        prefs.edit { putBoolean("hidden_status_initialized", initialized) }
    }

    fun isHiddenStatusInitialized(pdfId: Long): Boolean {
        val prefs = getPdfPrefs(pdfId)
        return prefs.getBoolean("hidden_status_initialized", false)
    }

    // ===== 데이터 정리 =====
    fun clearPdfData(pdfId: Long) {
        val prefs = getPdfPrefs(pdfId)
        prefs.edit { clear() }
        Log.d(TAG, "PDF $pdfId 데이터 정리 완료")
    }
}