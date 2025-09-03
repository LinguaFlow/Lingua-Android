package com.yju.presentation.view.pdf.adapter

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.presentation.BuildConfig
import com.yju.presentation.view.quiz.card.WordCardFragment

/**
 * ViewPager2에 표시될 단어 카드 어댑터
 * - 위치 정보 관리 개선
 */
class WordCardAdapter(
    private val activity: FragmentActivity,
    private val words: List<KanjiDetailModel>
) : FragmentStateAdapter(activity) {

    companion object {
        private const val TAG = "WordCardAdapter"
    }

    // 단어 ID 캐시 - 메모리 및 성능 최적화
    private val wordIdCache = HashMap<Int, Long>(words.size)

    init {
        // 초기화 시 ID 미리 계산
        words.forEachIndexed { index, word ->
            val id = generateWordId(word)
            wordIdCache[index] = id
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "위치 $index: ${word.kanji} (ID: ${word.vocabularyBookOrder}) -> 키: $id")
            }
        }
    }

    /**
     * 단어 고유 ID 생성
     */
    private fun generateWordId(word: KanjiDetailModel): Long {
        val idFromWord = word.vocabularyBookOrder.toLong().coerceAtLeast(1L)
        val kanjiHash = word.kanji.hashCode()
        val vocabularyOrderHash = word.vocabularyBookOrder

        // 고유 해시 생성
        val combinedHash = (kanjiHash * 31 + vocabularyOrderHash).toLong().and(0x00000000FFFFFFFFL)
        return (idFromWord shl 32) or combinedHash
    }

    override fun getItemCount(): Int = words.size

    /**
     * 프래그먼트 생성
     */
    override fun createFragment(position: Int): Fragment {
        // 범위 검사
        if (position !in words.indices) {
            Log.e(TAG, "유효하지 않은 위치 요청: $position (유효 범위: 0-${words.size - 1})")
            return Fragment()
        }

        val word = words[position]
        Log.d(TAG, "createFragment: 위치 $position, 단어 ${word.kanji} (ID: ${word.vocabularyBookOrder})")

        // 위치 정보 포함하여 프래그먼트 생성
        return WordCardFragment.newInstance(word, position)
    }

    /**
     * 각 항목의 고유 ID - 캐시 활용으로 성능 향상
     */
    override fun getItemId(position: Int): Long {
        return wordIdCache[position] ?: position.toLong()
    }

    /**
     * 항목 재사용 가능 여부 - 캐시 활용
     */
    override fun containsItem(itemId: Long): Boolean {
        return wordIdCache.values.contains(itemId)
    }

}