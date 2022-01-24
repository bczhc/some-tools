package pers.zhc.tools.charucd

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.char_ucd_lookup_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R

/**
 * @author bczhc
 */
class CharLookupActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.char_ucd_lookup_activity)

        val inputET = input_et!!.editText
        val button = btn!!

        button.setOnClickListener {
            val codepoint = inputET.text.toString().toInt(16)
            val intent = Intent(this, CharUcdActivity::class.java)
            intent.putExtra(CharUcdActivity.EXTRA_CODEPOINT, codepoint)
            startActivity(intent)
        }
    }
}