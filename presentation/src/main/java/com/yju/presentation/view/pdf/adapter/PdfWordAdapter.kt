package com.yju.presentation.view.pdf.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yju.domain.kanji.model.KanjiDetailModel
import com.yju.presentation.databinding.ItemPdfWordBinding

/**
 * 일본어 단어를 표시하는 RecyclerView 어댑터
 * - 선택 모드 지원 (단어 선택 및 다중 작업)
 * - 숨김 기능 (일반 단어장에서 모르는 단어로 이동된 단어 숨김)
 */
class PdfWordAdapter(
    private val onItemClick: (KanjiDetailModel) -> Unit,
    private val onItemLongClick: (KanjiDetailModel, Boolean) -> Unit
) : ListAdapter<KanjiDetailModel, PdfWordAdapter.WordViewHolder>(WordDiffCallback()) {

    companion object {
        private const val TAG = "PdfWordAdapter"
    }

    // 상태 관리
    private var selectionMode = false
    private val selectedItems = mutableMapOf<String, KanjiDetailModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemPdfWordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = getItem(position)
        holder.bind(word, position, selectionMode, isSelected(word))
        Log.d(TAG, "아이템 바인딩: 위치 $position, 단어: ${word.kanji}, 선택 모드: $selectionMode")
    }

    fun isInSelectionMode() = selectionMode

    fun enterSelectionMode() {
        if (!selectionMode) {
            selectionMode = true
            selectedItems.clear()
            Log.d(TAG, "선택 모드 진입")
            // 모든 개별 항목에 대해 변경 알림
            for (i in 0 until itemCount) {
                notifyItemChanged(i)
            }
        }
    }


    fun exitSelectionMode(): List<KanjiDetailModel> {
        val selectedList = getSelectedItems()
        if (selectionMode) {
            selectionMode = false
            selectedItems.clear()
            Log.d(TAG, "선택 모드 종료: 선택된 ${selectedList.size}개 항목 제거")
            // 모든 개별 항목에 대해 변경 알림
            for (i in 0 until itemCount) {
                notifyItemChanged(i)
            }
        }
        return selectedList
    }

    fun getSelectedItems(): List<KanjiDetailModel> {
        return selectedItems.values.toList()
    }

    fun selectAll() {
        if (selectionMode) {
            val previouslySelected = HashSet(selectedItems.keys)
            selectedItems.clear()
            val changedPositions = mutableListOf<Int>()
            for (i in 0 until itemCount) {
                val item = getItem(i)
                if (item != null) {
                    val key = getItemKey(item)
                    selectedItems[key] = item
                    if (!previouslySelected.contains(key)) {
                        changedPositions.add(i)
                    }
                }
            }
            for (position in changedPositions) {
                notifyItemChanged(position)
            }
        }
    }


    fun clearSelection() {
        if (selectionMode) {
            val selectedPositions = mutableListOf<Int>()
            for (i in 0 until itemCount) {
                val item = getItem(i)
                if (item != null && isSelected(item)) {
                    selectedPositions.add(i)
                }
            }
            selectedItems.clear()
            // 이전에 선택됐던 항목만 업데이트
            for (position in selectedPositions) {
                notifyItemChanged(position)
            }
        }
    }


    fun isSelected(item: KanjiDetailModel): Boolean {
        return selectedItems.containsKey(getItemKey(item))
    }

    fun toggleSelection(item: KanjiDetailModel): Boolean {
        val key = getItemKey(item)
        val wasSelected = selectedItems.containsKey(key)

        if (wasSelected) {
            selectedItems.remove(key)
            Log.d(TAG, "항목 선택 해제: ${item.kanji}")
        } else {
            selectedItems[key] = item
            Log.d(TAG, "항목 선택: ${item.kanji}")
        }

        // 중요: 해당 항목 위치를 찾아 갱신
        val position = currentList.indexOfFirst { getItemKey(it) == key }
        if (position != -1) {
            notifyItemChanged(position)
        }

        return !wasSelected // 새로운 상태 반환
    }


    private fun getItemKey(item: KanjiDetailModel): String = "${item.vocabularyBookOrder}:${item.kanji}"


    inner class WordViewHolder(private val binding: ItemPdfWordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // 클릭 리스너 설정
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (selectionMode) {
                        // 선택 모드: 아이템 선택/해제 토글
                        toggleSelection(item)
                    } else {
                        // 일반 모드: 클릭 이벤트 전달
                        onItemClick(item)
                    }
                }
            }

            // 롱클릭 리스너 설정
            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)

                    // 선택 모드가 아니면 진입 및 현재 아이템 선택
                    if (!selectionMode) {
                        enterSelectionMode()
                        toggleSelection(item)

                        // 콜백으로 선택 모드 진입 알림
                        Log.d(TAG, "롱클릭 발생: ${item.kanji} - 선택 모드 진입")
                        onItemLongClick(item, true)
                    }
                    return@setOnLongClickListener true
                }
                false
            }
        }

        /**
         * 뷰홀더에 데이터 바인딩
         */
        fun bind(word: KanjiDetailModel, position: Int, isSelectionMode: Boolean, isItemSelected: Boolean) {
            with(binding) {
                // 변수 바인딩
                vm = word
                this.position = position
                selectionMode = isSelectionMode
                selected = isItemSelected

                // XML에서 사용할 수 있도록 설정 (선택 사항)
                eventHolder = this@WordViewHolder

                // 즉시 바인딩 적용 (중요: UI 업데이트 지연 방지)
                executePendingBindings()

                // 디버그 로그 - 선택 모드 확인용
                if (isSelectionMode) {
                    Log.d(TAG, "아이템 바인딩 - 선택 모드: ${word.kanji}, 선택됨: $isItemSelected, 위치: $position")
                }
            }
        }
    }

    class WordDiffCallback : DiffUtil.ItemCallback<KanjiDetailModel>() {
        override fun areItemsTheSame(oldItem: KanjiDetailModel, newItem: KanjiDetailModel): Boolean {
            return oldItem.vocabularyBookOrder == newItem.vocabularyBookOrder && oldItem.kanji == newItem.kanji
        }

        override fun areContentsTheSame(oldItem: KanjiDetailModel, newItem: KanjiDetailModel): Boolean {
            return oldItem == newItem
        }
    }
}