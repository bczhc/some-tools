package pers.zhc.tools.words

import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.utils.ToastUtils

class DialogShowActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDialog()
    }

    private fun showDialog() {
        val dialog = DialogUtil.createPromptDialog(this, R.string.words_label, { et, _ ->
            ToastUtils.show(this, et.text)
            finish()
        }, { _, _ ->
            finish()
        })
        dialog.setOnCancelListener {
            finish()
        }
        dialog.show()
    }
}
