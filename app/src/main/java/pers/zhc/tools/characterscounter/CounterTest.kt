package pers.zhc.tools.characterscounter

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.ScrollEditText

/**
 * @author bczhc
 */
class CounterTest : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textEditText = findViewById<ScrollEditText>(R.id.text).editText
        val output = findViewById<TextView>(R.id.output)
        val counter = CharactersCounter()

        class MyTW : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                counter.clearResult()
                counter.count(s.toString())
                output.text = counter.getResultJson()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        }
        textEditText.addTextChangedListener(MyTW())
    }

}