package pers.zhc.tools.utils

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
