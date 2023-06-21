package pers.zhc.tools.utils

/**
 * @author bczhc
 */
class SpinLatch {
    @Volatile
    private var stop = false
    fun await() {
        @Suppress("ControlFlowWithEmptyBody")
        while (!stop);
    }

    fun suspend() {
        stop = false
    }

    /* alias for `suspend` */
    fun prepare() {
        stop = false
    }

    fun stop() {
        stop = true
    }
}
