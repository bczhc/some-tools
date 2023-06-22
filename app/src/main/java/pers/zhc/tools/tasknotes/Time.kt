package pers.zhc.tools.tasknotes

import java.io.Serializable
import java.util.*

class Time : Serializable {
    var minuteOfDay = 0

    val hour get() = minuteOfDay / 60

    val minute get() = minuteOfDay % 60

    fun setTime(hour: Int, minute: Int) {
        minuteOfDay = hour * 60 + minute
    }

    constructor(minuteOfDay: Int) {
        this.minuteOfDay = minuteOfDay
    }

    constructor(hour: Int, minute: Int) {
        setTime(hour, minute)
    }

    constructor(time: Date) {
        val calendar = Calendar.getInstance().apply {
            this.time = time
        }
        setTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }

    fun format(): String {
        val hour = this.hour.toString()
        val minute = this.minute.toString()
        return "${"0".repeat(2 - hour.length)}$hour:${"0".repeat(2 - minute.length)}$minute"
    }

    companion object {
        /**
         * in the local time zone
         */
        fun getTodayTimestampRange(): LongRange {
            val calendar = Calendar.getInstance().also { it.time = Date() }.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // 00:00:00
            val start = calendar.time.time
            // 24:00:00
            val end = calendar.also { it.set(Calendar.HOUR_OF_DAY, 24) }.time.time
            return start until end
        }
    }
}
