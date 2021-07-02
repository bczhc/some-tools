package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.diary_attachment_preview_activity.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common

/**
 * @author bczhc
 */
class DiaryAttachmentPreviewActivity : DiaryBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_preview_activity)

        val titleTV = title_tv!!
        val descriptionTV = description_tv!!
        val fileListLL = file_list_ll!!

        val intent = intent
        Common.doAssertion(intent.hasExtra(EXTRA_ATTACHMENT_ID))
        val attachmentId = intent.getLongExtra(EXTRA_ATTACHMENT_ID, -1)

        val statement = diaryDatabase.compileStatement(
            """SELECT title, description
FROM diary_attachment
WHERE id IS ?"""
        )
        statement.bind(1, attachmentId)
        val cursor = statement.cursor
        Common.doAssertion(cursor.step())
        val title = cursor.getText(0)
        val description = cursor.getText(1)
        statement.release()

        titleTV.text = getString(R.string.diary_attachment_preview_activity_title_is, title)
        descriptionTV.text = getString(R.string.diary_attachment_preview_activity_description_is, description)

        val statement2 = diaryDatabase.compileStatement(
            """SELECT identifier
FROM diary_attachment_file_reference
WHERE attachment_id IS ?""",
            arrayOf(attachmentId)
        )
        val cursor2 = statement2.cursor
        while (cursor2.step()) {
            val identifier = cursor2.getText(0)
            val filePreviewView = FileLibraryActivity.getFilePreviewView(this, diaryDatabase, identifier)
            filePreviewView.setOnClickListener {
                val startIntent = Intent(this, FileLibraryFileDetailActivity::class.java)
                startIntent.putExtra(FileLibraryFileDetailActivity.EXTRA_IDENTIFIER, identifier)
                startActivity(startIntent)
            }
            fileListLL.addView(filePreviewView)
        }
        statement2.release()
    }

    companion object {
        /**
         * long intent extra
         */
        const val EXTRA_ATTACHMENT_ID = "attachmentId"
    }
}