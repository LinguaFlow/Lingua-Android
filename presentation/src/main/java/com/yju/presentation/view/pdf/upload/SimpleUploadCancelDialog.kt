// SimpleUploadCancelDialog.kt
package com.yju.presentation.view.pdf.upload

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.core.graphics.drawable.toDrawable
import com.yju.presentation.R

class SimpleUploadCancelDialog(private val context: Context) {
    private var dialog: AppCompatDialog? = null
    private var onCancelListener: (() -> Unit)? = null
    private var progressText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var statusText: TextView? = null

    fun show(onCancel: (() -> Unit)? = null) {
        dismiss()
        onCancelListener = onCancel

        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_simple_upload_cancel, null)

        dialog = AppCompatDialog(context).apply {
            setContentView(dialogView)
            setCancelable(false)

            window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setDimAmount(0.7f)
            }

            progressText = findViewById(R.id.tvProgress)
            progressBar = findViewById(R.id.progressBar)


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

    fun dismiss() {
        try {
            dialog?.takeIf { it.isShowing }?.dismiss()
        } catch (e: Exception) {
            Log.e("SimpleUploadCancelDialog", "dismiss error: ${e.message}")
        } finally {
            dialog = null
            onCancelListener = null
            progressText = null
            progressBar = null
            statusText = null
        }
    }
}