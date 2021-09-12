package pers.zhc.tools.utils

/**
 * @author bczhc
 */
abstract class SharedRef<T> {
    private var objRc: RefCountHolder<T>? = null

    protected abstract fun create(): T

    protected abstract fun close(obj: T)

    fun newRefOrCreate(): Ref<T> {
        if (objRc == null) {
            val created = create()
            objRc = object : RefCountHolder<T>(created) {
                override fun onClose(obj: T) {
                    close(obj)
                }
            }
        }
        val newRef = objRc!!.newRef()
        return Ref(newRef) {
            countDown()
        }
    }

    private fun countDown() {
        objRc?.let {
            it.releaseRef()
            if (it.isAbandoned) {
                objRc = null
            }
        }
    }

    class Ref<T>(private val obj: T, private val onCountDown: () -> Unit) {
        private val released = false

        fun release() {
            onCountDown()
        }

        fun get(): T {
            if (released) {
                throw RuntimeException("Already released")
            }
            return obj
        }
    }
}