package pers.zhc.tools.words

import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.utils.ToastUtils

class DialogShowActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WordsMainActivity.checkAndInitDatabase()
        showDialog()
    }

    private fun showDialog() {
        val dialog = DialogUtil.createPromptDialog(this, R.string.words_label, { et, _ ->

            val database = WordsMainActivity.database!!

            val input = et.text.toString()
            val hasRecord = database.hasRecord("SELECT word\nFROM word\nWHERE word IS ?", arrayOf(input))

            if (hasRecord) {
                ToastUtils.show(this, R.string.words_already_have_word)
                finish()
                return@createPromptDialog
            }

            database.execBind(
                """INSERT INTO word(word, addition_time)
VALUES (?, ?)""",
                arrayOf(input, System.currentTimeMillis())
            )

            ToastUtils.show(this, R.string.adding_succeeded)

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
