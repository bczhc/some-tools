package pers.zhc.tools.charstat

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.commit
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.CharStatActivityBinding

class CharStatActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = CharStatActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val editText = bindings.editText.editText

        val fragmentManager = supportFragmentManager

        editText.doAfterTextChanged {
            val statFragment = CharStatFragment(editText.text.toString())
            fragmentManager.commit {
                replace(R.id.container, statFragment)
            }
        }
    }
}