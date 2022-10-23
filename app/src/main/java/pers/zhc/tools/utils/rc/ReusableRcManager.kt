package pers.zhc.tools.utils.rc

import pers.zhc.tools.utils.LangUtils.Companion.nullMap

abstract class ReusableRcManager<T> {
    protected abstract fun create(): T

    protected abstract fun release(obj: T)

    private var rcHolder: RcHolder<T>? = null

    // get the underlying reference with reference counting
    // if the held reference has been already abandoned, re-create it
    fun getRefOrCreate(): Ref<T> {
        if (rcHolder == null) {
            // re-create the object
            rcHolder = object : RcHolder<T>(create()) {
                override fun release(obj: T) {
                    this@ReusableRcManager.release(obj)
                    rcHolder = null
                }
            }
        }
        return rcHolder!!.newRef()
    }

    fun getRefCount(): Int {
        return rcHolder.nullMap { it.refCount } ?: 0
    }
}