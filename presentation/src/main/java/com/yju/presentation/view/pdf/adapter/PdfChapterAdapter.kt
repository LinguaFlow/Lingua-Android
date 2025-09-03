package com.yju.presentation.view.pdf.adapter

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.yju.presentation.R
import com.yju.presentation.base.BaseListAdapter
import com.yju.presentation.base.BaseViewHolder
import com.yju.presentation.databinding.ItemPageBinding
import java.util.regex.Pattern


class PdfChapterAdapter(
    private val onClick: (String) -> Unit
) : BaseListAdapter<String>(
    layoutResId = R.layout.item_page,
    diffCallback = PAGE_DIFF_CALLBACK
) {
    // 페이지별 단어 수를 저장하는 맵
    var wordCountsByPage = emptyMap<Int, Int>()
    private var lastClickTime = 0L
    private val MIN_CLICK_INTERVAL = 800L // 800ms

    inner class ChapterEventHandler {
        fun onChapterClick(chapter: String) {
            val currentTime = System.currentTimeMillis()

            // 중복 클릭 방지
            if (currentTime - lastClickTime < MIN_CLICK_INTERVAL) {
                Log.d("PdfChapterAdapter", "중복 클릭 방지: $chapter")
                return
            }
            lastClickTime = currentTime

            Log.d("PdfChapterAdapter", "챕터 클릭: $chapter")
            onClick(chapter)
        }
    }

    init {
        eventHolder = ChapterEventHandler()
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        try {
            // 실제 데이터 위치 계산 (헤더가 있는 경우 position-1)
            val dataPosition = if (headerItem != null && position == 0) -1 else
                if (headerItem != null) position - 1 else position
            // 헤더가 아니고 유효한 위치인 경우에만 처리
            if (dataPosition >= 0 && dataPosition < currentList.size) {
                // BaseViewHolder에서 ItemPageBinding 가져오기
                val binding = holder.getBinding<ItemPageBinding>()
                // 현재 챕터 문자열 가져오기
                val chapter = currentList[dataPosition]

                // 중요: 클릭 리스너 다시 설정
                binding.root.setOnClickListener {
                    val currentTime = System.currentTimeMillis()
                    // 중복 클릭 방지
                    if (currentTime - lastClickTime < MIN_CLICK_INTERVAL) {
                        Log.d("PdfChapterAdapter", "중복 클릭 방지: $chapter")
                        return@setOnClickListener
                    }
                    lastClickTime = currentTime

                    Log.d("PdfChapterAdapter", "챕터 클릭: $chapter")
                    onClick(chapter)
                }

                // 사용자 친화적인 제목 표시
                formatChapterTitle(binding, chapter, dataPosition)
                // 해당 페이지의 단어 수 표시
                val wordCount = wordCountsByPage[dataPosition] ?: calculateWordCount(chapter)
                binding.tvWordCount.text = "${wordCount}개의 단어"
            }
        } catch (e: Exception) {
            Log.e("PdfChapterAdapter", "onBindViewHolder 오류: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun formatChapterTitle(binding: ItemPageBinding, chapter: String, position: Int) {
        val pagePattern = Pattern.compile("(\\d+):(\\d+)")
        val pageMatcher = pagePattern.matcher(chapter)

        if (pageMatcher.find()) {
            val pageNum = pageMatcher.group(1)
            val subIndex = pageMatcher.group(2)?.toIntOrNull() ?: 1
            // 페이지 번호는 원 안에 표시
            binding.tvPageNumber.text = pageNum
            // 더 사용자 친화적인 제목 형식으로 변환
//            val start = (subIndex - 1) * 10 + 1
//            val end = start + 9
            binding.tvChapterTitle.text = "${pageNum}페이지 단어 묶음 ${subIndex}"
        }
        // "K:1" 형식 인식 (아는 단어 페이지)
        else if (chapter.startsWith("K:")) {
            val pageIndex = chapter.substring(2).toIntOrNull() ?: 1
            binding.tvPageNumber.text = pageIndex.toString()

            binding.tvChapterTitle.text = "아는 단어 묶음 ${pageIndex}"
        }
        // 기존 형식 처리
        else {
            val knownPattern = Pattern.compile("Known (\\d+) ~ (\\d+)")
            val knownMatcher = knownPattern.matcher(chapter)

            val chapterPattern = Pattern.compile("Chapter (\\d+) ~ (\\d+)")
            val chapterMatcher = chapterPattern.matcher(chapter)

            val pageOnlyPattern = Pattern.compile("Page (\\d+)")
            val pageOnlyMatcher = pageOnlyPattern.matcher(chapter)

            when {
                knownMatcher.find() -> {
                    binding.tvPageNumber.text = (position + 1).toString()
                    binding.tvChapterTitle.text = "아는 단어 ${knownMatcher.group(1)}~${knownMatcher.group(2)}"
                }
                chapterMatcher.find() -> {
                    binding.tvPageNumber.text = (position + 1).toString()
                    binding.tvChapterTitle.text = "단어 ${chapterMatcher.group(1)}~${chapterMatcher.group(2)}"
                }
                pageOnlyMatcher.find() -> {
                    binding.tvPageNumber.text = pageOnlyMatcher.group(1)
                    binding.tvChapterTitle.text = "${pageOnlyMatcher.group(1)}페이지 단어"
                }
                else -> {
                    binding.tvPageNumber.text = (position + 1).toString()
                    binding.tvChapterTitle.text = chapter
                }
            }
        }
    }

    /**
     * 챕터 문자열에서 단어 수 계산 (기본 방식)
     */
    private fun calculateWordCount(chapter: String): Int {
        // "1:2" 형식 처리
        val pagePattern = Pattern.compile("(\\d+):(\\d+)")
        val pageMatcher = pagePattern.matcher(chapter)
        if (pageMatcher.find()) {
            return 10 // 기본값으로 10개 반환 (실제 값은 wordCountsByPage에서 가져옴)
        }

        // "Chapter 1 ~ 10" 형식 처리
        val pattern = Pattern.compile("Chapter (\\d+) ~ (\\d+)")
        val matcher = pattern.matcher(chapter)
        if (matcher.find()) {
            val start = matcher.group(1)?.toIntOrNull() ?: 1
            val end = matcher.group(2)?.toIntOrNull() ?: start
            return end - start + 1
        }

        // "Unknown 1 ~ 10" 형식 처리
        val knownPattern = Pattern.compile("Unknown (\\d+) ~ (\\d+)")
        val knownMatcher = knownPattern.matcher(chapter)
        if (knownMatcher.find()) {
            val start = knownMatcher.group(1)?.toIntOrNull() ?: 1
            val end = knownMatcher.group(2)?.toIntOrNull() ?: start
            return end - start + 1
        }

        // 기본값
        return 10
    }

    companion object {
        private val PAGE_DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }
}