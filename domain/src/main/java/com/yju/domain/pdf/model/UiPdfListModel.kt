package com.yju.domain.pdf.model

interface UiPdfListModel {
    interface OnItemClickListener {
        fun onItemClick(id: Long)
    }
}