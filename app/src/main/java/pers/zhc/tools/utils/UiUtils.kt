package pers.zhc.tools.utils

import android.content.Context

fun Context.runOnUiThread(action: () -> Unit) {
    Common.runOnUiThread(this, action)
}

fun Context.awaitRunOnUiThread(action: () -> Unit) {
    val latch = NotifyLatch()
    this.runOnUiThread {
        action()
        latch.unlatch()
    }
    latch.await()
}

fun Context.spinAwaitRunOnUiThread(action: () -> Unit) {
    var done = false
    this.runOnUiThread {
        action()
        done = true
    }
    @Suppress("ControlFlowWithEmptyBody")
    while (!done);
}
