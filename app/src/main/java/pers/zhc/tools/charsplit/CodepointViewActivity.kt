package pers.zhc.tools.charsplit

import android.os.Bundle
import androidx.fragment.app.commitNow
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R

class CodepointViewActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_container)

        val text = intent.getStringExtra(EXTRA_TEXT) ?: throw RuntimeException("No text provided")

        val codepointFragment = CodepointFragment(defaultText = text)

        supportFragmentManager.commitNow {
            replace(R.id.container, codepointFragment)
        }
    }

    companion object {
        /**
         * string intent extra
         */
        const val EXTRA_TEXT = "text"
    }
}