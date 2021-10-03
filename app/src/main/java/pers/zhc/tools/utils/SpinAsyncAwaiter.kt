package pers.zhc.tools.utils

/**
 * @author bczhc
 */
class SpinAsyncAwaiter {
    private var stop = false
    private val notifier = {
        stop = true
    }

    fun await(f: Function) {
        f(notifier)
        @Suppress("ControlFlowWithEmptyBody")
        while (!stop);
    }
}

typealias Notifier = () -> Unit

typealias Function = (Notifier) -> Unit
