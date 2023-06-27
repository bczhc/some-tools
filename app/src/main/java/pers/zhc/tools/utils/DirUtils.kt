package pers.zhc.tools.utils

import android.content.Context
import java.io.File

/**
 * Compat version of [Context.getExternalFilesDir]
 */
fun Context.externalFilesDir(): File {
    return File(Common.getExternalStoragePath(this))
}
