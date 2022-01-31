package pers.zhc.tools.test

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import kotlinx.android.synthetic.main.regular_expression_test_layout.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.RegexUtils.Companion.capture

class RegExpTest : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.regular_expression_test_layout)

        val inputET = input_et!!.editText
        val regexInputView = regex_input!!
        val resultTV = tv!!

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