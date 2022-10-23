package pers.zhc.tools.utils.rc

class Ref<T>(obj: T, private val countdown: () -> Unit) {
    private var obj: T? = obj

    fun release() {
        countdown()
    }

    fun get(): T {
        obj?.let {
            return it
        }
        throw RuntimeException("Already released")
    }
}