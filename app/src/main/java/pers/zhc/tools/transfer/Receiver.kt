package pers.zhc.tools.transfer

import pers.zhc.tools.jni.JNI

/**
 * @author bczhc
 */
class Receiver {
    companion object {
        fun startAsyncReceive(port: Short, onResult: (msg: String?, error: Boolean) -> Unit) {
            JNI.Transfer.startAsyncReceive(port) { msg, error ->
                onResult(msg, error)
            }
        }

        enum class Status(val statusValue: Int) {
            OK(0);

            companion object {
                fun from(statusValue: Int): Status {
                    return when (statusValue) {
                        0 -> {
                            OK
                        }
                        else -> {
                            throw RuntimeException("Unknown status value")
                        }
                    }
                }
            }
        }
    }
}
