package pers.zhc.tools.utils

import java.text.SimpleDateFormat
import java.util.*

fun Date.toCalendar(date: Date): Calendar {
    return Calendar.getInstance().apply {
        time = date
    }
}

fun Date.isToday(): Boolean {
    return CalendarUtils.isToday(this)
}

fun Date.format(pattern: String, locale: Locale = Locale.getDefault(), timeZone: TimeZone? = null): String {
    val format = SimpleDateFormat(pattern, locale)
    timeZone?.let { format.timeZone = timeZone }
    return format.format(this)
}

fun Date.toUtcIso8601(): String {
    val tz = TimeZone.getTimeZone("UTC")
    return this.format("yyyy-MM-dd'T'HH:mm'Z'", timeZone = tz)
}

object CalendarUtils {
    /**
     * in the local time zone
     */
    fun isToday(time: Date): Boolean {
        val calendar = Calendar.getInstance().apply {
            // now
            this.time = Date()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 00:00:00
        val start = calendar.time.time
        // 24:00:00
        val end = calendar.also { it.add(Calendar.HOUR_OF_DAY, 24) }.time.time
        return time.time in start until end
    }
}
