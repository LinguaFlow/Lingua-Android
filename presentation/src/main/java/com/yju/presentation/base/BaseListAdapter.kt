package com.yju.presentation.base

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

open class BaseListAdapter<T : Any>(
    @LayoutRes private val layoutResId: Int,
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseViewHolder>(diffCallback) {
    var eventHolder: Any? = null

    private var headerSize: Int = 0

    @LayoutRes
    var headerLayoutResId: Int? = null
        set(value) {
            if (value == field) return
            field = value
            headerSize = if (value == null) 0 else 1
        }
    var headerItem: T? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(layoutResId = viewType, parent = parent)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item: Any? =
            if (isHeaderPosition(position)) headerItem
            else getItem(position - headerSize)
        holder.onBind(item, eventHolder)
    }

    override fun getItemCount(): Int = super.getItemCount() + headerSize

    override fun getItemViewType(position: Int): Int {
        if (isHeaderPosition(position)) {
            return headerLayoutResId
                ?: error("headerLayoutResId must be set when headerSize > 0")
        }
        val item = getItem(position - headerSize)
        return if (item is BaseItemViewType) item.itemLayoutResId else layoutResId
    }

    private fun isHeaderPosition(position: Int): Boolean = headerSize > 0 && position == 0
}