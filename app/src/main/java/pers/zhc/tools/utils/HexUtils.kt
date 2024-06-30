package pers.zhc.tools.utils

fun ByteArray.toHexString(separator: String = "") = joinToString(separator) { "%02x".format(it) }
