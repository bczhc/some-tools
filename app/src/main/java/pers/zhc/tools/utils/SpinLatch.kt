package pers.zhc.tools.utils

/**
 * @author bczhc
 */
class SpinLatch {
    @Volatile
    private var stop = false
    fun await() {
        while (true) {
            if (stop) {
                break
            }
        }
    }

    fun suspend() {
        stop = false
    }

    fun stop() {
        stop = true
    }
}