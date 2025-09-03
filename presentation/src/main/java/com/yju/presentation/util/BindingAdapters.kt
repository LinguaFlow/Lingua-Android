package com.yju.presentation.util


import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter

@BindingAdapter("layout_constraintHeight_percent")
fun View.setHeightPercent(percent: Float) {
    (layoutParams as? ConstraintLayout.LayoutParams)?.let { lp ->
        lp.matchConstraintPercentHeight = percent
        layoutParams = lp
    }
}