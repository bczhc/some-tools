package pers.zhc.tools.utils

import java.util.*

/**
 * @author bczhc
 */
class IntDate {
    var year = 0
    var month = 0
    var day = 0

    constructor(dateInt: Int) {
        set(dateInt)
    }

    constructor(date: IntArray) {
        set(date)
    }

    constructor(date: Date) {
        set(date)
    }

    class ParseDateException : RuntimeException {
        constructor(message: String) : super(message)
        constructor(cause: Throwable) : super(cause)
    }

    /**
     * [date]: e.g.: "2021.3.12" or "2021.03.02"
     */
    constructor(date: String) {
        val split = date.split(Regex("\\."))
        if (split.size != 3) {
            throw ParseDateException("Invalid part size")
        }
        val ints = IntArray(3)
        split.forEachIndexed { index, s ->
            try {
                val i = s.toInt()
                ints[index] = i
            } catch (e: NumberFormatException) {
                throw ParseDateException(e)
            }
        }
        val calendar = Calendar.getInstance()
        calendar.set(ints[0], ints[1] - 1, ints[2])
        set(calendar.time)
    }

    fun set(dateInt: Int) {
        year = dateInt / 10000
        month = dateInt / 100 % 100
        day = dateInt % 100
    }

    fun set(date: IntArray) {
        year = date[0]
        month = date[1]
        day = date[2]
    }

    fun set(date: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = date
        year = calendar[Calendar.YEAR]
        month = calendar[Calendar.MONTH] + 1
        day = calendar[Calendar.DAY_OF_MONTH]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val myDate = other as IntDate
        if (year != myDate.year) return false
        return if (month != myDate.month) false else day == myDate.day
    }

    override fun hashCode(): Int {
        var result = year
        result = 31 * result + month
        result = 31 * result + day
        return result
    }

    override fun toString(): String {
        return "$year.$month.$day"
    }

    private fun add0(a: Int): String {
        return if (a < 10) "0$a" else a.toString()
    }

    val dateIntString: String
        get() = add0(year) + add0(month) + add0(day)
    val dateInt: Int
        get() = year * 10000 + month * 100 + day
}