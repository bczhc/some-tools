package pers.zhc.tools.test.signals

import pers.zhc.tools.jni.JNI

class Signal(val name: String, val int: Int) {
    fun raise() {
        JNI.Signals.raise(this.int)
    }

    companion object {
        val signals: List<Signal>

        init {
            val names = JNI.Signals.getSignalNames()
            val ints = JNI.Signals.getSignalInts()
            signals = names.zip(ints.toTypedArray()).map { p ->
                Signal(p.first!!, p.second)
            }
        }
    }
}