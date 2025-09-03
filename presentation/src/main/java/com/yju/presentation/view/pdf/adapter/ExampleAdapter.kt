package com.yju.presentation.view.pdf.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yju.domain.known.model.TranslationExampleModel
import com.yju.presentation.databinding.ItemExampleBinding

/**
 * 예문 목록을 표시하는 RecyclerView 어댑터
 */
class ExampleAdapter(
    private val onPlayClick: (String) -> Unit
) : ListAdapter<TranslationExampleModel, ExampleAdapter.ExampleViewHolder>(EXAMPLE_DIFF_CALLBACK) {

    private var currentPosition = RecyclerView.NO_POSITION
    private var isAudioPlaying = false

    inner class ExampleEventHandler {
        fun onItemClick(position: Int) {
            handleItemSelection(position)
        }
        fun onPlayClick(japanese: String) {
            onPlayClick(japanese)
        }
        fun isSelected(position: Int): Boolean =
            position == currentPosition
        fun isPlaying(position: Int): Boolean =
            position == currentPosition && isAudioPlaying
        fun getTotalCount(): Int = currentList.size
    }

    /**
     * ViewHolder 클래스
     */
    inner class ExampleViewHolder(
        private val binding: ItemExampleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TranslationExampleModel, position: Int) {
            binding.apply {
                vm = item
                eventHolder = ExampleEventHandler()
                this.position = position  // position 바인딩
                executePendingBindings()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExampleViewHolder {
        val binding = ItemExampleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExampleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExampleViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    /**
     * 부분 업데이트 처리
     */
    override fun onBindViewHolder(
        holder: ExampleViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            // 부분 업데이트 처리
            for (payload in payloads) {
                when (payload) {
                    PAYLOAD_AUDIO_STATE_CHANGED -> {
                        holder.bind(getItem(position), position)
                    }
                }
            }
        }
    }

    /**
     * 오디오 재생 상태 설정
     */
    fun setAudioPlayingState(isPlaying: Boolean) {
        if (isAudioPlaying != isPlaying) {
            isAudioPlaying = isPlaying
            // 현재 선택된 아이템만 업데이트
            if (currentPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(currentPosition, PAYLOAD_AUDIO_STATE_CHANGED)
            }
        }
    }


    /**
     * 아이템 선택 처리
     */
    private fun handleItemSelection(position: Int) {
        if (position != currentPosition && position in 0 until itemCount) {
            val oldPosition = currentPosition
            currentPosition = position

            // 이전 선택 항목 업데이트
            if (oldPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPosition)
            }

            // 새 선택 항목 업데이트
            notifyItemChanged(currentPosition)
        }
    }

    companion object {
        private const val PAYLOAD_AUDIO_STATE_CHANGED = "audio_state_changed"

        private val EXAMPLE_DIFF_CALLBACK = object : DiffUtil.ItemCallback<TranslationExampleModel>() {
            override fun areItemsTheSame(
                oldItem: TranslationExampleModel,
                newItem: TranslationExampleModel
            ): Boolean {
                return oldItem.japanese == newItem.japanese
            }

            override fun areContentsTheSame(
                oldItem: TranslationExampleModel,
                newItem: TranslationExampleModel
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}