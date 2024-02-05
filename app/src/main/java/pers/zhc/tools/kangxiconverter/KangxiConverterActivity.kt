package pers.zhc.tools.kangxiconverter

import android.os.Bundle
import android.view.View
import android.widget.Button
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.kangxiconverter.KangxiConverter.KangXi2Normal
import pers.zhc.tools.kangxiconverter.KangxiConverter.markKangxiRadicalsEditText
import pers.zhc.tools.kangxiconverter.KangxiConverter.markNormalHansEditText
import pers.zhc.tools.kangxiconverter.KangxiConverter.normal2KangXi
import pers.zhc.tools.views.ScrollEditText

class KangxiConverterActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kangxi_converter_activity)
        val kangxiRadicals2NormalHansBtn = findViewById<Button>(R.id.kangxi_radicals_to_normal_hans)
        val normalHans2KangxiRadicalsBtn = findViewById<Button>(R.id.normal_hans_to_kangxi_radicals)
        val inputEt = findViewById<ScrollEditText>(R.id.input_et)
        val outputEt = findViewById<ScrollEditText>(R.id.output_et)
        kangxiRadicals2NormalHansBtn.setOnClickListener { v: View? ->
            val input = inputEt.text.toString()
            val output = KangXi2Normal(input)
            outputEt.setText(output)
            markKangxiRadicalsEditText(inputEt.editText)
            markNormalHansEditText(outputEt.editText)
        }
        normalHans2KangxiRadicalsBtn.setOnClickListener { v: View? ->
            val input = inputEt.text.toString()
            val output = normal2KangXi(input)
            outputEt.setText(output)
            markNormalHansEditText(inputEt.editText)
            markKangxiRadicalsEditText(outputEt.editText)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.slide_out_bottom)
    }
}
