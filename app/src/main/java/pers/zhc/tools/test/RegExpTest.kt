package pers.zhc.tools.test

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class RegExpTest : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.regular_expression_test_layout)
        val textET = findViewById<EditText>(R.id.text)
        val regexET = findViewById<EditText>(R.id.regex)
        regexET.setBackgroundResource(R.drawable.edittext_right)
        val tv = findViewById<TextView>(R.id.tv)
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                try {
                    val matcher = Pattern.compile(regexET.text.toString()).matcher(textET.text.toString())
                    val sb = StringBuilder()
                    while (matcher.find()) {
                        // java doesn't count the initial group (index 0, the whole subject input)
                        val groupCount = matcher.groupCount() + 1
                        val groups = ArrayList<String>()
                        for (i in 0 until groupCount) {
                            groups.add(matcher.group(i)!!)
                        }
                        sb.append(groups.joinToString())
                        sb.append('\n')
                    }
                    tv.text = sb.toString()
                    regexET.setBackgroundResource(R.drawable.edittext_right)
                } catch (_: PatternSyntaxException) {
                    regexET.setBackgroundResource(R.drawable.edittext_wrong)
                }
            }
        }
        textET.addTextChangedListener(watcher)
        regexET.addTextChangedListener(watcher)
    }
}