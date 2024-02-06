package pers.zhc.tools.test

import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.jni.JNI

/**
 * @author bczhc
 */
class Demo : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val array = ByteArray(4)
        JNI.Char.encodeUTF8(0xd800, array, 0)
        println(array)
    }
}
