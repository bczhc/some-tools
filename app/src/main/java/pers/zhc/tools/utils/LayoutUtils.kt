package pers.zhc.tools.utils

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

fun View.setBaseLayoutSize(width: Int, height: Int) {
    this.layoutParams = ViewGroup.LayoutParams(width, height)
}

fun View.setBaseLayoutSizeMW() {
    this.setBaseLayoutSize(MATCH_PARENT, WRAP_CONTENT)
}

fun View.setBaseLayoutSizeMM() {
    this.setBaseLayoutSize(MATCH_PARENT, MATCH_PARENT)
}