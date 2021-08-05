package pers.zhc.tools.utils

/**
 * @author bczhc
 */
class RegexUtils {
    companion object {
        fun String.capture(regex: String): ArrayList<List<String>> {
            val list = ArrayList<List<String>>()
            Regex(regex).findAll(this).forEach {
                list.add(it.groupValues)
            }
            return list
        }
    }
}