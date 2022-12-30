package pers.zhc.tools.utils

import pers.zhc.tools.jni.JNI.Unicode.Grapheme

class GraphemeIterator(string: String) : Iterator<String> {
    private val addr = Grapheme.newIterator(string)

    override fun hasNext(): Boolean {
        return Grapheme.hasNext(addr)
    }

    override fun next(): String {
        return Grapheme.next(addr)
    }

    protected fun finalize() {
        if (addr != 0L) {
            Grapheme.release(addr)
        }
    }
}
