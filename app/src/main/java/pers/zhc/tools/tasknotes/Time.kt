package pers.zhc.tools.tasknotes

import java.util.Calendar
import java.util.Date

class Time {
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
}
