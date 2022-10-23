package pers.zhc.tools.utils.rc

abstract class RcHolder<T>(private val obj: T) {
    var refCount = 0
        private set
    private var abandoned = false

    protected abstract fun release(obj: T)

    fun newRef(): Ref<T> {
        checkAbandoned()
        ++refCount
        return Ref(obj, this::countdown)
    }

    private fun countdown() {
        checkAbandoned()
        --refCount
        if (refCount == 0) {
            abandoned = true
            release(obj)
        }
    }

    private fun checkAbandoned() {
        if (abandoned) throw RuntimeException("Abandoned!")
    }

    fun isAbandoned(): Boolean {
        return abandoned
    }
}