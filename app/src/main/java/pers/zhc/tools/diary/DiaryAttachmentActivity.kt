package pers.zhc.tools.diary

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.diary_attachment_activity.*
import kotlinx.android.synthetic.main.diary_attachment_preview_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.sqlite.Statement

class DiaryAttachmentActivity : DiaryBaseActivity() {
    private var pickMode: Boolean = false

    /**
     * -1 if no dateInt specified, which means this activity is started by [DiaryMainActivity], not [DiaryTakingActivity]
     * If [dateInt] is -1, the add action ([R.id.add] in [onOptionsItemSelected]) will show all the attachments.
     * Otherwise that add action will attach an attachment to a diary whose date is [dateInt]
     */
    private var dateInt: Int = -1
    private lateinit var linearLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_activity)

        this.linearLayout = ll!!

        val intent = intent
        dateInt = intent.getIntExtra("dateInt", -1)
        pickMode = intent.getBooleanExtra("pickMode", false)
        if (dateInt != -1) {
            val myDate = DiaryTakingActivity.MyDate(dateInt)
            val calendar = Calendar.getInstance()
            calendar.set(myDate.year, myDate.month - 1, myDate.day)
            val formatter = SimpleDateFormat.getPatternInstance(getString(R.string.date_format))
            val format = formatter.format(calendar.time)
            if (!pickMode) title = getString(R.string.attachment_with_date_concat, format)
        }

        checkAttachmentInfoRecord()

        showViews()
    }

    private fun showViews() {
        val statement: Statement
        if (dateInt == -1 || pickMode) {
            statement = diaryDatabase.compileStatement("SELECT *\nFROM diary_attachment")
        } else {
            statement = diaryDatabase.compileStatement("SELECT *\nFROM diary_attachment\nWHERE id IS (SELECT referred_attachment_id FROM diary_attachment_mapping WHERE diary_date IS ?);")
            statement.bind(1, dateInt)
        }
        val idColumnIndex = statement.getIndexByColumnName("id")
        val titleColumnIndex = statement.getIndexByColumnName("title")
        val descriptionColumnIndex = statement.getIndexByColumnName("description")

        val cursor = statement.cursor
        while (cursor.step()) {
            val title = cursor.getText(titleColumnIndex)
            val description = cursor.getText(descriptionColumnIndex)
            val id = cursor.getLong(idColumnIndex)

            val previewView = getPreviewView(title, description, id)
            this.linearLayout.addView(previewView)
        }
        statement.release()
    }

    private fun getPreviewView(title: String?, description: String?, id: Long): View? {
        val inflate = View.inflate(this, R.layout.diary_attachment_preview_view, null)
        val titleTV = inflate.title_tv
        val descriptionTV = inflate.description_tv

        titleTV.text = getString(R.string.title_is, title)
        descriptionTV.text = getString(R.string.description_is_text, description)

        inflate.setOnClickListener {
            if (pickMode) {
                val resultIntent = Intent()
                resultIntent.putExtra("pickedAttachmentId", id)
                setResult(0, resultIntent)
                finish()
            }
        }
        return inflate
    }

    private fun checkAttachmentInfoRecord() {
        val fileStoragePath = DiaryAttachmentSettingsActivity.getFileStoragePath(diaryDatabase)
        if (fileStoragePath == null) {
            // record "info_json" doesn't exists, then start to set it
            startActivity(Intent(this, DiaryAttachmentSettingsActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.diary_attachment_actionbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                if (dateInt == -1 || pickMode) {
                    val intent = Intent(this, DiaryAttachmentAddingActivity::class.java)
                    intent.putExtra("dateInt", dateInt)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, DiaryAttachmentActivity::class.java)
                    intent.putExtra("dateInt", dateInt)
                    intent.putExtra("pickMode", true)
                    startActivityForResult(intent, RequestCode.START_ACTIVITY_0)
                }
            }
            R.id.file_library -> {
                startActivity(Intent(this, FileLibraryActivity::class.java))
            }
            R.id.setting_btn -> {
                startActivity(Intent(this, DiaryAttachmentSettingsActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCode.START_ACTIVITY_0 -> {
                // no attachment picked
                data ?: return

                val pickedAttachmentId = data.getLongExtra("pickedAttachmentId", -1)
                if (pickedAttachmentId == -1L) throw AssertionError()
                attachAttachment(pickedAttachmentId)
                ToastUtils.show(this, R.string.adding_succeeded)
            }
            else -> {
            }
        }
    }

    /**
     * attach an attachment to diary
     */
    private fun attachAttachment(pickedAttachmentId: Long) {
        Common.doAssert(dateInt != -1)
        val statement =
            diaryDatabase.compileStatement("INSERT INTO diary_attachment_mapping(diary_date, referred_attachment_id)\nVALUES (?, ?)")
        statement.bind(1, dateInt)
        statement.bind(2, pickedAttachmentId)
        statement.step()
        statement.release()
    }
}