package pers.zhc.tools.diary

import android.os.Bundle
import android.view.*
import kotlinx.android.synthetic.main.diary_attachment_activity.*
import kotlinx.android.synthetic.main.diary_attachment_preview_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.diary.fragments.AttachmentFragment
import pers.zhc.tools.utils.*
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

        val attachmentFragment = AttachmentFragment(fromDiary, pickMode, dateInt)
        val manager = supportFragmentManager
        manager.beginTransaction().add(R.id.container, attachmentFragment).commit()
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