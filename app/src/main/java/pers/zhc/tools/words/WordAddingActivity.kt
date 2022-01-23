package pers.zhc.tools.words

import android.content.Intent
import android.os.Build
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class WordAddingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            ToastUtils.show(this, R.string.unsupported_function_toast)
            finish()
            return
        }
        val selectedText = (intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT) ?: run {
            ToastUtils.show(this, R.string.failed_to_get_content_toast)
            finish()
            return
        }).toString()

        WordsMainActivity.checkAndInitDatabase()
        val database = WordsMainActivity.database!!

        if (WordDatabase.checkExistence(database, selectedText)) {
            ToastUtils.show(this, R.string.words_already_have_word)
            finish()
            return
        }

        WordDatabase.addWord(database, selectedText)
        ToastUtils.show(this, R.string.adding_succeeded)
        finish()
    }
}