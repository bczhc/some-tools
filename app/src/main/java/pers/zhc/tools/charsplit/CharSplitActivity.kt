package pers.zhc.tools.charsplit

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.CharsSplitterActivityBinding
import pers.zhc.tools.utils.unreachable

/**
 * @author bczhc
 */
class CharSplitActivity : BaseActivity() {
    private val fragments = object {
        val codepoint = CodepointFragment()
        val grapheme = GraphemeFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = CharsSplitterActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val btg = bindings.btg.also { it.check(R.id.as_codepoint) }
        val inputET = bindings.inputEt

        // initial
        supportFragmentManager.commit { replace(R.id.container, fragments.codepoint) }
        var updateList = fragments.codepoint::updateList

        btg.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            when (checkedId) {
                R.id.as_codepoint -> {
                    supportFragmentManager.commitNow {
                        replace(R.id.container, fragments.codepoint)
                    }
                    updateList = fragments.codepoint::updateList.also { it(inputET.text.toString()) }
                }
                R.id.as_grapheme -> {
                    supportFragmentManager.commitNow {
                        replace(R.id.container, fragments.grapheme)
                    }
                    updateList = fragments.grapheme::updateList.also { it(inputET.text.toString()) }
                }
                else -> unreachable()
            }
        }

        inputET.doAfterTextChanged {
            updateList(it.toString())
        }
    }
}