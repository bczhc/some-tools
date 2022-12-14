package pers.zhc.tools.utils

fun <F, T> F?.nullMap(f: (F) -> T): T? {
    return f(this ?: return null)
}
