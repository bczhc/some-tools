package pers.zhc.tools.utils

fun String.capture(regex: String): List<List<String>> {
    return this.capture(Regex(regex))
}

fun String.capture(regex: Regex): List<List<String>> {
    return regex.findAll(this).map {
        it.groupValues
    }.toList()
}
