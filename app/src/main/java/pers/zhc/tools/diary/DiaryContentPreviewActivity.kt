package pers.zhc.tools.diary

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.diary_content_preview_activity.*
import kotlinx.android.synthetic.main.diary_record_stat_dialog.view.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.DialogUtils
import pers.zhc.tools.utils.DisplayUtil
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
        val edit = registerForActivityResult(DiaryTakingActivity.ActivityContract()) {result->
            val content = fetchContent(result.dateInt)
            contentTV.text = content
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_content_preview_activity)

        contentTV = content_tv!!
        contentTV.textSize = getEditTextTextSize(this)
        bottomAttachmentLL = bottom_attachment_ll!!

        val intent = intent
        val dateInt = intent.getIntExtra(EXTRA_DATE_INT, -1)
        if (dateInt == -1) throw RuntimeException("No dateInt provided.")
        this.dateInt = dateInt

        val content = fetchContent(dateInt)

        val tvText = if (intent.hasExtra(EXTRA_HIGHLIGHT_REGEX)) {
            val highlightRegex = intent.getSerializableExtra(EXTRA_HIGHLIGHT_REGEX) as Regex
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
                launchers.edit.launch(MyDate(dateInt))
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

        fun createDiaryRecordStatDialog(context: Context, database: DiaryDatabase, dateInt: Int): Dialog {
            return Dialog(context).apply {
                setTitle(R.string.diary_statistics_dialog_title)
                setContentView(View.inflate(context, R.layout.diary_record_stat_dialog, null).apply {
                    this.stat_content_tv!!.text = context.getString(
                        R.string.diary_record_statistics_dialog_content,
                        database.getCharsCount(dateInt)
                    )
                })
                DialogUtils.setDialogAttr(this, width = MATCH_PARENT)
            }
        }
    }
}