package pers.zhc.tools.utils

fun String.limitText(length: Int): String {
    return if (this.length > length) this.substring(0, 100) + "..." else this
}

/**
 * Returns: (index, length)
 *
 * Data is all processed in UTF-16
 */
fun String.indexesOf(needle: String): List<Pair<Int, Int>> {
    return buildList {
        var start = 0
        while (start < this@indexesOf.length) {
            val index = this@indexesOf.indexOf(needle, start)
            if (index == -1) break
            add(Pair(index, needle.length))
            start = index + needle.length
        }
    }
}

fun String.indexesOf(needles: List<String>): List<Pair<Int, Int>> {
    return needles.flatMap { this.indexesOf(it) }.distinct()
}

fun String.completeLeadingZeros(totalLength: Int): String {
    val s = this
    return if (s.length < totalLength) {
        "${"0".repeat(totalLength - s.length)}$s"
    } else s
}
