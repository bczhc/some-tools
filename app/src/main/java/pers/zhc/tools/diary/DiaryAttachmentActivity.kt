package pers.zhc.tools.diary

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import pers.zhc.tools.R

class DiaryAttachmentActivity : DiaryBaseActivity() {
    /**
     * -1 if no dateInt specified
     */
    private var dateInt: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_activity)

        val intent = intent
        dateInt = intent.getIntExtra("dateInt", -1)
        if (dateInt != -1) {
            val myDate = DiaryTakingActivity.MyDate(dateInt)
            val calendar = Calendar.getInstance()
            calendar.set(myDate.year, myDate.month - 1, myDate.day)
            val formatter = SimpleDateFormat.getPatternInstance(getString(R.string.date_format))
            val format = formatter.format(calendar.time)
            title = getString(R.string.attachment_with_date_concat, format)
        }

        checkAttachmentInfoRecord()
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
                val intent = Intent(this, DiaryAttachmentAddingActivity::class.java)
                intent.putExtra("dateInt", dateInt)
                startActivity(intent)
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
}