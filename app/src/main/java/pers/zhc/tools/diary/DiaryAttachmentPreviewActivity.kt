package pers.zhc.tools.diary

import android.os.Bundle
import androidx.core.content.ContextCompat
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

        val statement = diaryDatabase.compileStatement("SELECT *\nFROM diary_attachment\nWHERE id IS ?")
        statement.bind(1, attachmentId)
        val cursor = statement.cursor
        Common.doAssertion(cursor.step())
        val title = cursor.getText(statement.getIndexByColumnName("title"))
        val description = cursor.getText(statement.getIndexByColumnName("description"))
        statement.release()

        titleTV.text = getString(R.string.diary_attachment_preview_activity_title_is, title)
        descriptionTV.text = getString(R.string.diary_attachment_preview_activity_description_is, description)

        val statement2 = diaryDatabase.compileStatement(
            "SELECT *\nFROM diary_attachment_file_reference\nWHERE attachment_id IS ?",
            arrayOf(attachmentId)
        )
        val identifierColumnIndex = statement2.getIndexByColumnName("file_identifier")
        val cursor2 = statement2.cursor
        while (cursor2.step()) {
            val identifier = cursor2.getText(identifierColumnIndex)
            val filePreviewView = FileLibraryActivity.getFilePreviewView(this, diaryDatabase, identifier)
            filePreviewView.background = ContextCompat.getDrawable(this, R.drawable.view_stroke)
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