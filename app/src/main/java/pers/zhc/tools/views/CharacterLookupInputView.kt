package pers.zhc.tools.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import kotlinx.android.synthetic.main.character_lookup_input_dialog.view.*
import pers.zhc.tools.R
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.test.UnicodeTable
import pers.zhc.tools.utils.CodepointIterator
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.util.Assertion

/**
 * @author bczhc
 */
class CharacterLookupInputView : WrapLayout {
    private lateinit var codepointET: EditText

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        val inflate = View.inflate(context, R.layout.character_lookup_input_dialog, null)
        this.setView(inflate)

        codepointET = inflate.codepoint_et!!.editText
        val charET = inflate.char_et!!.editText

        var doSetText = false

        val setText = { func: () -> Unit ->
            doSetText = true
            func()
            doSetText = false
        }

        codepointET.doAfterTextChanged {
            if (doSetText) return@doAfterTextChanged

            val codepointInput = codepointET.text.toString()
            val codepoint = codepointInput.toIntOrNull(16).also {
                if (it == null || it !in 1..0x10FFFF) {
                    setText {
                        charET.setText("")
                    }
                    return@doAfterTextChanged
                }
            }!!
            val s = String(intArrayOf(codepoint), 0, 1)
            Assertion.doAssertion(JNI.Utf8.codepointLength(s) == 1)

            setText {
                charET.setText(s)
            }
        }
        charET.doAfterTextChanged {
            if (doSetText) return@doAfterTextChanged

            val charInput = charET.text.toString().also {
                if (it.isEmpty()) {
                    setText {
                        codepointET.setText("")
                    }
                    return@doAfterTextChanged
                }
            }
            val codepoints: List<Int>
            try {
                codepoints = CodepointIterator(charInput).toList()
            } catch (e: Exception) {
                ToastUtils.showError(context, R.string.please_enter_correct_value_toast, e)
                setText {
                    charET.text.clear()
                    codepointET.text.clear()
                }
                return@doAfterTextChanged
            }
            Assertion.doAssertion(codepoints.isNotEmpty())
            if (codepoints.size > 1) {
                val s = String(intArrayOf(codepoints[0]), 0, 1)
                setText {
                    charET.setText(s)
                    charET.setSelection(charET.length())
                }
            }
            Assertion.doAssertion(JNI.Utf8.codepointLength(charET.text.toString()) == 1)
            setText {
                codepointET.setText(UnicodeTable.completeCodepointNum(codepoints[0].toString(16)))
            }
        }
    }

    fun getCodepoint(): Int? {
        return codepointET.text.toString().toIntOrNull(16)
    }
}
