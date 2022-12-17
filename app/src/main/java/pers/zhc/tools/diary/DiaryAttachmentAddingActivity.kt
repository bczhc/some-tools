package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.diary_attachment_adding_activity.*
import pers.zhc.tools.R
import pers.zhc.tools.diary.fragments.FileLibraryFragment
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.stepBind

class DiaryAttachmentAddingActivity : DiaryBaseActivity() {
    private lateinit var descriptionET: EditText
    private lateinit var titleET: EditText
    private val fileIdentifierList = ArrayList<String>()
    private lateinit var fileListLL: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.diary_attachment_adding_activity)
        titleET = title_et!!
        fileListLL = file_list_ll!!
        descriptionET = description_et!!
        val createAttachmentBtn = create_attachment_btn!!
        val pickFileBtn = pick_file_btn

        pickFileBtn.setOnClickListener {
            val filePickerIntent = Intent(this, FileLibraryActivity::class.java)
            filePickerIntent.putExtra(FileLibraryActivity.EXTRA_PICK_MODE, true)
            // pick file from the file library
            startActivityForResult(filePickerIntent, RequestCode.START_ACTIVITY_0)
        }

        createAttachmentBtn.setOnClickListener {
            val attachmentId = createAttachment()
            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_RESULT_ATTACHMENT_ID, attachmentId)
            setResult(0, resultIntent)
            ToastUtils.show(this, R.string.creating_succeeded)
            finish()
        }
    }

    /**
     * returns attachmentId
     */
    private fun createAttachment(): Long {
        val attachmentId = System.currentTimeMillis()
        val database = diaryDatabase.database
        database.beginTransaction()

        database.execBind(
            """INSERT INTO diary_attachment(id, title, description)
VALUES (?, ?, ?)""",
            arrayOf(
                attachmentId,
                titleET.text.toString(),
                descriptionET.text.toString()
            )
        )

        val statement =
            database.compileStatement(
                """INSERT INTO diary_attachment_file_reference(attachment_id, identifier)
VALUES (?, ?)"""
            )
        fileIdentifierList.forEach {
            statement.stepBind(arrayOf(attachmentId, it))
        }
        statement.release()
        database.commit()

        return attachmentId
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // no file picked
        data ?: return

        when (requestCode) {
            RequestCode.START_ACTIVITY_0 -> {
                // pick file from the file library
                val identifier = data.getStringExtra(FileLibraryActivity.EXTRA_PICKED_FILE_IDENTIFIER)!!
                if (fileIdentifierList.contains(identifier)) {
                    ToastUtils.show(this, R.string.diary_attachment_library_duplicate_toast)
                    return
                }
                val filePreviewView = FileLibraryFragment.getFilePreviewView(this, diaryDatabase, identifier)
                filePreviewView.background = ContextCompat.getDrawable(this, R.drawable.view_stroke)
                fileListLL.addView(filePreviewView)
                fileIdentifierList.add(identifier)
            }

            else -> {

            }
        }
    }

    companion object {
        /**
         * intent string extra
         * the id of the added attachment
         */
        const val EXTRA_RESULT_ATTACHMENT_ID = "resultAttachmentId"
    }
}