package pers.zhc.tools.magic

import pers.zhc.tools.jni.JNI

/**
 * @author bczhc
 */
class Magic {
    private var addr: Long = 0

    init {
        addr = JNI.Magic.init(JNI.Magic.MAGIC_NONE)
    }

    fun load(databasePath: String) {
        JNI.Magic.load(addr, databasePath)
    }

    fun file(path: String): String {
        return JNI.Magic.file(addr, path)
    }

    fun close() {
        JNI.Magic.close(addr)
    }
}