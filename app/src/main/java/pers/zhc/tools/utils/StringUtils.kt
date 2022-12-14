package pers.zhc.tools.utils

fun String.limitText(length: Int): String {
    return if (this.length > length) this.substring(0, 100) + "..." else this
}
