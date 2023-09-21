package pers.zhc.tools.digest

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.DigestMainBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.ClipboardUtils

class MainActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = DigestMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val resultTv = bindings.resultTv

        bindings.textEt.editText.doAfterTextChanged {
            val hash = JNI.Digest.sha256(it.toString().toByteArray())
            resultTv.text = hash
        }

        resultTv.setOnClickListener {
            ClipboardUtils.putWithToast(this, resultTv.text.toString())
        }
    }
}
