package pers.zhc.tools.diary

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContract
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.views.ScrollEditText

/**
 * @author bczhc
 */
class DiaryFileLibraryEditTextActivity : BaseActivity() {

    private lateinit var scrollEditText: ScrollEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scrollEditText = ScrollEditText(this)
        val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
        scrollEditText.layoutParams = ViewGroup.LayoutParams(matchParent, matchParent)
        scrollEditText.id = R.id.scroll_et
        setContentView(scrollEditText)

        val intent = intent
        intent.getStringExtra(EXTRA_INITIAL_TEXT)?.let {
            scrollEditText.setText(it)
        }
    }

    override fun finish() {
        val intent = Intent()
        intent.putExtra(EXTRA_RESULT, scrollEditText.text.toString())
        setResult(0, intent)
        super.finish()
    }

    companion object {
        /**
         * result intent string extra
         */
        const val EXTRA_RESULT = "result"

        /**
         * intent string extra
         */
        const val EXTRA_INITIAL_TEXT = "result"
    }

    /**
     * input: initial text
     *
     * output: result text
     */
    class TextEditContract : ActivityResultContract<String?, String>() {
        override fun createIntent(context: Context, input: String?): Intent {
            return Intent(context, DiaryFileLibraryEditTextActivity::class.java).apply {
                input?.let { putExtra(EXTRA_INITIAL_TEXT, it) }
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String {
            intent!!
            return intent.getStringExtra(EXTRA_RESULT)!!
        }

    }
}
