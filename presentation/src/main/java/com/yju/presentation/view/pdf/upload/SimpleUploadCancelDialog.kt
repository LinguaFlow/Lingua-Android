package com.yju.presentation.view.pdf.upload

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import com.yju.presentation.R
import androidx.core.graphics.drawable.toDrawable


class SimpleUploadCancelDialog(private val context: Context) {
    private var dialog: AppCompatDialog? = null
    private var onCancelListener: (() -> Unit)? = null
    private var progressText: TextView? = null

    fun show(onCancel: (() -> Unit)? = null) {
        dismiss() // 기존 다이얼로그가 있다면 닫기
        onCancelListener = onCancel
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_simple_upload_cancel, null)

        dialog = AppCompatDialog(context).apply {
            setContentView(dialogView)
            setCancelable(false)

            window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setDimAmount(0.7f)
            }

            // 진행률 텍스트 참조
            progressText = findViewById(R.id.tvProgress)
            updateProgress("업로드 중... 100%")

            // 취소 버튼 설정
            findViewById<Button>(R.id.btnCancel)?.apply {
                setOnClickListener {
                    onCancelListener?.invoke()
                    dismiss()
                }
            }
            show()
        }
    }

    fun updateProgress(text: String) {
        progressText?.text = text
    }

    fun dismiss() {
        dialog?.takeIf { it.isShowing }?.dismiss()
        dialog = null
        onCancelListener = null
        progressText = null
    }
}