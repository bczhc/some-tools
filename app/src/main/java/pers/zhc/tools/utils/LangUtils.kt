package pers.zhc.tools.utils

class LangUtils {
    companion object {
        fun <F, T> F?.nullMap(f: (F) -> T): T? {
            return f(this ?: return null)
        }
    }
}