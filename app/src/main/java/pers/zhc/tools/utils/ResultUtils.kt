package pers.zhc.tools.utils

import android.content.Context

fun <T> Result<T>.toastOnFailure(ctx: Context): Result<T> {
    this.onFailure {
        val message = (it.message ?: "").limitText(50)
        ToastUtils.show(ctx, message)
    }
    return this
}
