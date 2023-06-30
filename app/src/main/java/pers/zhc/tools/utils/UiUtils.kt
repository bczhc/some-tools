package pers.zhc.tools.utils

fun runOnUiThread(action: () -> Unit) = Common.runOnUiThread(action)

fun awaitRunOnUiThread(action: () -> Unit) {
    val latch = NotifyLatch()
    runOnUiThread {
        action()
        latch.unlatch()
    }
    latch.await()
}

fun spinAwaitRunOnUiThread(action: () -> Unit) {
    var done = false
    runOnUiThread {
        action()
        done = true
    }
    @Suppress("ControlFlowWithEmptyBody")
    while (!done);
}
