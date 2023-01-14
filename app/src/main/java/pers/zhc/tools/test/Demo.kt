package pers.zhc.tools.test

import android.os.Bundle
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.thread

/**
 * @author bczhc
 */
class Demo : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val body = thread {
            runBlocking {
                HttpClient().request("https://httpbin.org/ip").bodyAsText()
            }
        }.join()
        ToastUtils.show(this, body)

        JavaDemo.Enum.A.ordinal

        for (enum in arrayOf(JavaDemo.Enum.A, JavaDemo.Enum.B)) {
            when (enum) {
                JavaDemo.Enum.A -> TODO()
                JavaDemo.Enum.B -> TODO()
            }
        }
    }
}