package com.yju.presentation.view.pdf.adapter

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.yju.presentation.view.pdf.normal.NormalChapterFragment
import com.yju.presentation.view.pdf.known.KnownWordChapterFragment
import kotlinx.coroutines.CoroutineScope
import java.lang.ref.WeakReference


class ChapterPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val pdfId: Long,
    private val coroutineScope: CoroutineScope
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    companion object {
        private const val TAG = "ChapterPagerAdapter"
        const val TAB_NORMAL = 0
        const val TAB_UNKNOWN = 1
        private const val TAB_COUNT = 2
    }

    private val fragments = mutableMapOf<Int, WeakReference<Fragment>>()
    private var currentTabPosition = TAB_NORMAL

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            TAB_NORMAL -> NormalChapterFragment.newInstance(pdfId)
            TAB_UNKNOWN -> KnownWordChapterFragment.newInstance(pdfId)
            else -> throw IllegalArgumentException("Invalid position: $position")
        }

        fragments[position] = WeakReference(fragment)
        Log.d(TAG, "Fragment 생성: 위치 $position")
        return fragment
    }

    fun refreshPage(position: Int) {
        fragments[position]?.get()?.let { fragment ->
            when (fragment) {
                is NormalChapterFragment -> fragment.refreshData()
                is KnownWordChapterFragment -> fragment.refreshData()
            }
        }
    }

    fun refreshAllPages() {
        for (i in 0 until TAB_COUNT) {
            refreshPage(i)
        }
    }

    fun setCurrentTab(position: Int): Boolean {
        if (position == currentTabPosition) return false
        currentTabPosition = position
        return true
    }

    override fun getItemId(position: Int): Long = pdfId * 10 + position

    override fun containsItem(itemId: Long): Boolean {
        val position = (itemId % 10).toInt()
        val fragmentPdfId = itemId / 10
        return fragmentPdfId == pdfId && position in 0 until TAB_COUNT
    }

    fun release() {
        fragments.clear()
    }
}