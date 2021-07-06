package pers.zhc.tools.diary

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import pers.zhc.tools.R
import pers.zhc.tools.diary.fragments.AttachmentFragment
import java.text.SimpleDateFormat
import java.util.*

class DiaryAttachmentActivity : DiaryBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_activity)

        var dateInt = -1
        if (intent.hasExtra(AttachmentFragment.EXTRA_DATE_INT)) {
            dateInt = intent.getIntExtra(AttachmentFragment.EXTRA_DATE_INT, -1)
        }
        val fromDiary = intent.getBooleanExtra(AttachmentFragment.EXTRA_FROM_DIARY, false)
        val pickMode = intent.getBooleanExtra(AttachmentFragment.EXTRA_PICK_MODE, false)

        if (fromDiary) {
            supportActionBar?.title =
                SimpleDateFormat(getString(R.string.diary_attachment_with_date_format_title), Locale.US).format(
                    getDateFromDateInt(dateInt)
                )
        }

        val attachmentFragment = AttachmentFragment(fromDiary, pickMode, dateInt)

        val manager = supportFragmentManager
        manager.beginTransaction().add(R.id.container, attachmentFragment).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.diary_attachment_actionbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return false
    }

    companion object {
        /**
         * intent long extra
         * When [EXTRA_PICK_MODE] extra is `true`, this extra will be used in the result intent extras.
         */
        const val EXTRA_PICKED_ATTACHMENT_ID = "pickedAttachmentId"

        /**
         * intent integer extra
         */
        const val EXTRA_DATE_INT = "dateInt"

        /**
         * intent boolean extra
         * See [AttachmentFragment.pickMode].
         */
        const val EXTRA_PICK_MODE = "pickMode"

        /**
         * intent boolean extra
         * See [AttachmentFragment.fromDiary] property.
         */
        const val EXTRA_FROM_DIARY = "fromDiary"
    }
}