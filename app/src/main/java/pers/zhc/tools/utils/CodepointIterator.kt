package pers.zhc.tools.utils

import pers.zhc.tools.jni.JNI

/**
 * @author bczhc
 */
class CodepointIterator(s: String): Iterator<Int> {
    private val addr = JNI.Unicode.Codepoint.newIterator(s)

    override fun hasNext(): Boolean {
        return JNI.Unicode.Codepoint.hasNext(addr)
    }

    override fun next(): Int {
        return JNI.Unicode.Codepoint.next(addr)
    }

    private fun release() {
        JNI.Unicode.Codepoint.release(addr)
    }

    /**
     * Method for finalizing this object for JVM
     */
    protected fun finalize() {
        if (addr != 0L) {
            release()
        }
    }
}