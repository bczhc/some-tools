package pers.zhc.tools.test

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.RegularExpressionTestLayoutBinding
import pers.zhc.tools.utils.capture

class RegExpTest : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = RegularExpressionTestLayoutBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val inputET = bindings.inputEt.editText
        val regexInputView = bindings.regexInput
        val resultTV = bindings.tv

        val update = { regex: Regex? ->
            if (regex != null) {
                val captured = inputET.text.toString().capture(regex)
                resultTV.text = captured.joinToString("\n", "", "") {
                    it.joinToString(prefix = "[", postfix = "]")
                }
            } else {
                resultTV.text = ""
            }
        }

        inputET.doAfterTextChanged {
            update(regexInputView.regex)
        }
        regexInputView.regexChangeListener = {
            update(it)
        }
    }
}