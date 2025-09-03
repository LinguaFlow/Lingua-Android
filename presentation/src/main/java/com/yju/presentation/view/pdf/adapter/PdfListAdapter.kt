package com.yju.presentation.view.pdf.adapter

import androidx.recyclerview.widget.DiffUtil
import com.yju.domain.kanji.model.KanjiModel
import com.yju.presentation.R
import com.yju.presentation.base.BaseListAdapter

/**
 * PDF 목록을 표시하는 어댑터
 */
class PdfListAdapter(
    private val onItemDeleteClick: (Long) -> Unit, // 명확한 이름으로 변경
    private val onItemViewClick: ((Long) -> Unit)? = null,
    var isDeleteMode: Boolean = true
) : BaseListAdapter<KanjiModel>(
    layoutResId = R.layout.item_pdf_list,
    diffCallback = PDF_COMPARATOR
) {
    /**
     * 이벤트 핸들러 클래스
     */
    inner class EventHandler {
        // XML에서 호출하는 메서드 이름과 정확히 일치시킴
        fun deleteMode(): Boolean = isDeleteMode

        // 항목 전체 클릭 처리 - 상세 보기로 이동
        fun onViewClick(id: Long) {
            android.util.Log.d("PdfListAdapter", "항목 보기 클릭: PDF ID $id")
            onItemViewClick?.invoke(id)
        }

        // 삭제 버튼 클릭 처리
        fun onDeleteClick(id: Long) {
            android.util.Log.d("PdfListAdapter", "삭제 버튼 클릭: PDF ID $id")
            // 상위 콜백 직접 호출 (this@PdfListAdapter.onItemDeleteClick 대신 간단히 사용)
            onItemDeleteClick(id)
        }
    }

    init {
        // 이벤트 핸들러 초기화
        eventHolder = EventHandler()
    }

    // 목록 갱신 시 메모리 참조 문제 방지
    override fun submitList(list: List<KanjiModel>?) {
        super.submitList(list?.let { ArrayList(it) })
    }

    companion object {
        private val PDF_COMPARATOR = object : DiffUtil.ItemCallback<KanjiModel>() {
            override fun areItemsTheSame(oldItem: KanjiModel, newItem: KanjiModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: KanjiModel, newItem: KanjiModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}