package pers.zhc.tools.utils

class StringUtils {
    companion object {
        fun limitText(s: String): String {
            return if (s.length > 100) s.substring(0, 100) + "..." else s
        }
    }
}