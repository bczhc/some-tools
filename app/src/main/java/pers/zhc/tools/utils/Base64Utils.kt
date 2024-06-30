package pers.zhc.tools.utils

import android.util.Base64


fun ByteArray.encodeBase64(): String {
    return Base64.encodeToString(this, 0)
}

fun String.decodeBase64(): ByteArray {
    return Base64.decode(this, 0)
}
