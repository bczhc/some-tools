package pers.zhc.tools.utils

import android.content.Intent

fun Intent.getLongExtraOrNull(name: String): Long? {
    return getLongExtra(name, -1).takeIf { hasExtra(name) }
}

fun Intent.getIntExtraOrNull(name: String): Int? {
    return getIntExtra(name, -1).takeIf { hasExtra(name) }
}

fun Intent.getBooleanExtraOrNull(name: String): Boolean? {
    return getBooleanExtra(name, false).takeIf { hasExtra(name) }
}
