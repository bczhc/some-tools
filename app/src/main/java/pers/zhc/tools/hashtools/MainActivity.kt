package pers.zhc.tools.hashtools

import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import pers.zhc.tools.databinding.HashToolsMainBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.ClipboardUtils
import pers.zhc.tools.utils.toHexString
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = HashToolsMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val charset = StandardCharsets.UTF_8

        var iterationNum = 1
        bindings.apply {
            calcBtn.setOnClickListener {
                val text = textEt.text.toString()
                val textBytes = text.toByteArray(charset)

                out1.text = Base64.encodeToString(textBytes, Base64.NO_WRAP)
                out2.text = sha256(textBytes, iterationNum).toHexString()
                out3.text = Base64.encodeToString(sha256(textBytes, iterationNum), Base64.NO_WRAP)
            }

            arrayOf(Pair(out1, out1Copy), Pair(out2, out2Copy), Pair(out3, out3Copy)).forEach { p ->
                p.second.setOnClickListener {
                    ClipboardUtils.putWithToast(this@MainActivity, p.first.text.toString())
                }
            }

            @Suppress("SetTextI18n")
            iterationNumEt.doAfterTextChanged {
                val num = it.toString().toIntOrNull() ?: 1
                if (num == 1) {
                    hintTv.text = "hash=sha256"
                } else {
                    hintTv.text = "hash=nest(sha256, %d)".format(num)
                }
                iterationNum = num
            }
        }
    }

    private fun sha256(data: ByteArray, iteration: Int = 1): ByteArray {
        return JNI.Digest.sha256(data, iteration)
    }
}
