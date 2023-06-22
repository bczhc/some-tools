package pers.zhc.tools.test

import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.jni.JNI.BZip3
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class Demo : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = "hello, world".toByteArray()
        val bytes = BZip3.compress(data, 1048576)
        val string = String(BZip3.decompress(bytes))

        ToastUtils.show(this, string)
        JNI.JniDemo.call2()
    }
}
