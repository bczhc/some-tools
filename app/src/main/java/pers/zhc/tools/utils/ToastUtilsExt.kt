package pers.zhc.tools.utils

import android.content.Context
import androidx.annotation.StringRes

fun Context.toast(text: String) {
    ToastUtils.show(this, text)
}


fun Context.toast(@StringRes resId: Int) {
    ToastUtils.show(this, resId)
}
