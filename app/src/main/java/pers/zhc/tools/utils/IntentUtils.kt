package pers.zhc.tools.utils

import android.content.Intent

fun Intent.getLongExtraOrNull(name: String): Long? {
    if (this.hasExtra(name)) {
        return this.getLongExtra(name, -1)
    }
    return null
}
