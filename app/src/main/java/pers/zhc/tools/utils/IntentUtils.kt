package pers.zhc.tools.utils

import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import java.io.Serializable
import kotlin.reflect.KClass

fun Intent.getLongExtraOrNull(name: String): Long? {
    return getLongExtra(name, -1).takeIf { hasExtra(name) }
}

fun Intent.getIntExtraOrNull(name: String): Int? {
    return getIntExtra(name, -1).takeIf { hasExtra(name) }
}

fun Intent.getBooleanExtraOrNull(name: String): Boolean? {
    return getBooleanExtra(name, false).takeIf { hasExtra(name) }
}

fun <T : Serializable> Intent.getSerializableExtra(name: String, c: KClass<T>): T? {
    return if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
        this.getSerializableExtra(name, c.java)
    } else {
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        this.getSerializableExtra(name) as T?
    }
}
