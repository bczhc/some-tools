package pers.zhc.tools.kangxiconverter

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.KangxiConverterActivityBinding
import pers.zhc.tools.utils.codepointChars
import pers.zhc.tools.utils.indexesOf

class KangxiConverterActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = KangxiConverterActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val inputEt = bindings.inputEt
        val outputEt = bindings.outputEt

        bindings.kangxiRadicalsToNormalHans.setOnClickListener {
            val output = KangxiConverter.kangxiRadicals2normal(inputEt.text.toString())
            outputEt.setText(output)
            markKangxiRadicalsEditText(inputEt.editText)
            markNormalHansEditText(outputEt.editText)
        }
        bindings.normalHansToKangxiRadicals.setOnClickListener {
            val output = KangxiConverter.normal2KangxiRadicals(inputEt.text.toString())
            outputEt.setText(output)
            markNormalHansEditText(inputEt.editText)
            markKangxiRadicalsEditText(outputEt.editText)
        }
    }

    companion object {
        fun markKangxiRadicalsEditText(et: EditText) {
            val inputText = et.text.toString()
            val spannableString = SpannableString(inputText)

            for (pair in inputText.indexesOf(KangxiConverter.KANGXI_RADICALS.codepointChars())) {
                val start = pair.first
                val end = start + pair.second
                val colorSpan = ForegroundColorSpan(Color.RED) // 高亮颜色
                spannableString.setSpan(colorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            et.setText(spannableString)
            et.setSelection(inputText.length) // 将光标移至末尾
        }

        fun markNormalHansEditText(et: EditText) {
            val inputText = et.text.toString()
            val spannableString = SpannableString(inputText)

            for (pair in inputText.indexesOf(KangxiConverter.NORMAL_HANS.codepointChars())) {
                val start = pair.first
                val end = start + pair.second
                val colorSpan = ForegroundColorSpan(Color.GREEN) // 高亮颜色
                spannableString.setSpan(colorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            et.setText(spannableString)
            et.setSelection(inputText.length) // 将光标移至末尾
        }
    }
}
