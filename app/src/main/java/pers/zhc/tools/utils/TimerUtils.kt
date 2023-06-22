package pers.zhc.tools.utils

import java.util.*

fun Timer.scheduleNow(task: () -> Unit) {
    this.schedule(task, 0)
}

fun Timer.schedule(task: () -> Unit, delay: Long) {
    this.schedule(object : TimerTask() {
        override fun run() {
            task()
        }
    }, delay)
}
