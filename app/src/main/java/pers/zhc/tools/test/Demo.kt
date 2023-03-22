package pers.zhc.tools.test

import android.os.Bundle
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.jni.JNI.BZip3
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.thread

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
    }
}
