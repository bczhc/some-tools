package pers.zhc.tools.diary

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import pers.zhc.tools.R
import pers.zhc.tools.databinding.DiaryContentPreviewActivityBinding
import pers.zhc.tools.utils.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author bczhc
 */
class DiaryContentPreviewActivity : DiaryBaseActivity() {
    private lateinit var bottomAttachmentLL: LinearLayout
    private lateinit var contentTV: TextView
    private var dateInt: Int = -1

    private val launchers = object {
        val edit = registerForActivityResult(DiaryTakingActivity.ActivityContract()) { result ->
            val content = fetchContent(result.dateInt)
            contentTV.text = content
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = DiaryContentPreviewActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        contentTV = bindings.contentTv
        contentTV.textSize = getEditTextTextSize(this)
        bottomAttachmentLL = bindings.bottomAttachmentLl

        val intent = intent
        val dateInt = intent.getIntExtra(EXTRA_DATE_INT, -1)
        if (dateInt == -1) throw RuntimeException("No dateInt provided.")
        this.dateInt = dateInt

        val content = fetchContent(dateInt)

        val tvText = if (intent.hasExtra(EXTRA_HIGHLIGHT_REGEX)) {
            val highlightRegex = intent.getSerializableExtra(EXTRA_HIGHLIGHT_REGEX, Regex::class)!!
            val ranges = highlightRegex.findAll(content).map {
                it.range
            }
            SpannableString(content).apply {
                ranges.forEach {
                    setSpan(
                        ForegroundColorSpan(Color.RED),
                        it.first,
                        it.last + 1 /* this parameter is exclusive */,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        } else {
            content
        }
        contentTV.text = tvText

        showBottomAttachment()

        val formatter = SimpleDateFormat(getString(R.string.diary_preview_with_date_format_title), Locale.US)
        val format = formatter.format(getDateFromDateInt(dateInt))
        title = format
    }

    private fun showBottomAttachment() {
        // TODO: 4/4/21 Not yet implemented.
    }

    private fun fetchContent(dateInt: Int): String {
        return diaryDatabase.queryDiaryContent(dateInt)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.diary_content_preview_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit -> {
                launchers.edit.launch(IntDate(dateInt))
            }

            R.id.attachment -> {
                val intent = Intent(this, DiaryAttachmentActivity::class.java)
                intent.putExtra(DiaryAttachmentActivity.EXTRA_FROM_DIARY, true)
                intent.putExtra(EXTRA_DATE_INT, dateInt)
                startActivity(intent)
            }

            R.id.statistics -> {
                createDiaryRecordStatDialog(this, diaryDatabase, dateInt).show()
            }

            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_DATE_INT, dateInt)
        setResult(0, resultIntent)

        super.finish()
    }

    companion object {
        /**
         * in sp
         */
        fun getEditTextTextSize(ctx: Context) = DisplayUtil.px2sp(ctx, EditText(ctx).textSize).toFloat()

        /**
         * intent integer extra
         */
        const val EXTRA_DATE_INT = "dateInt"

        /**
         * intent serializable extra, optional
         */
        const val EXTRA_HIGHLIGHT_REGEX = "highlightRegex"
    }

    class ActivityContract : ActivityResultContract<ActivityContract.Input, IntDate>() {
        class Input(
            val dateInt: Int,
            val highlightPattern: Regex?
        )

        override fun createIntent(context: Context, input: Input): Intent {
            return Intent(context, DiaryContentPreviewActivity::class.java).apply {
                putExtra(EXTRA_DATE_INT, input.dateInt)
                input.highlightPattern?.let { putExtra(EXTRA_HIGHLIGHT_REGEX, it) }
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): IntDate {
            intent!!
            return IntDate(intent.getIntExtraOrNull(EXTRA_DATE_INT)!!)
        }
    }
}
