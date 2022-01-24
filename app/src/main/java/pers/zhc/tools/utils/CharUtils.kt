package pers.zhc.tools.utils

import pers.zhc.tools.jni.JNI

/**
 * @author bczhc
 */
class CharUtils {
    companion object {
        fun getEncodedUtf8(codepoint: Int): ByteArray {
            val buf = ByteArray(JNI.Char.getUtf8Len(codepoint))
            JNI.Char.encodeUTF8(codepoint, buf, 0)
            return buf
        }

        fun getEncodedUtf16(codepoint: Int): ShortArray {
            val buf = ShortArray(JNI.Char.getUtf16Len(codepoint))
            JNI.Char.encodeUTF16(codepoint, buf, 0)
            return buf
        }
    }
}