package pers.zhc.tools.utils

import android.content.Context

fun Context.runOnUiThread(action: () -> Unit) {
    Common.runOnUiThread(this, action)
}
