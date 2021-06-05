package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.diary_attachment_adding_activity.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.ToastUtils
import java.util.*

class DiaryAttachmentAddingActivity : DiaryBaseActivity() {
    private lateinit var descriptionET: EditText
    private lateinit var titleET: EditText
    private lateinit var fileIdentifierList: LinkedList<String>
    private lateinit var fileListLL: LinearLayout
    private var attachmentId = -1L;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.diary_attachment_adding_activity)
        titleET = title_et!!
        fileListLL = file_list_ll!!
        descriptionET = description_et!!
        val createAttachmentBtn = create_attachment_btn!!
        val pickFileBtn = pick_file_btn

        fileIdentifierList = LinkedList<String>()

        val intent = intent
        Common.doAssertion(intent.hasExtra(EXTRA_ATTACHMENT_ID))
        attachmentId = intent.getLongExtra(EXTRA_ATTACHMENT_ID, -1)
        pickFileBtn.setOnClickListener {
            val filePickerIntent = Intent(this, FileLibraryActivity::class.java)
            filePickerIntent.putExtra("pick", true)
            // pick file from the file library
            startActivityForResult(filePickerIntent, RequestCode.START_ACTIVITY_0)
        }

        createAttachmentBtn.setOnClickListener {
            val attachmentId = createAttachment()
            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_ATTACHMENT_ID, attachmentId)
            setResult(0, resultIntent)
            ToastUtils.show(this, R.string.creating_succeeded)
            finish()
        }
    }

    private fun createAttachmentAttachedDiary() {
        createAttachment()
    }

    /**
     * returns attachmentId
     */
    private fun createAttachment(): Long {
        val attachmentId = System.currentTimeMillis()
        diaryDatabase.beginTransaction()

        var statement =
            diaryDatabase.compileStatement("INSERT INTO diary_attachment(id, title, description)\nVALUES (?, ?, ?)")
        statement.bind(1, attachmentId)
        statement.bindText(2, titleET.text.toString())
        statement.bindText(3, descriptionET.text.toString())
        statement.step()
        statement.release()

        statement =
            diaryDatabase.compileStatement("INSERT INTO diary_attachment_file_reference(attachment_id, identifier)\nVALUES (?, ?)")
        fileIdentifierList.forEach {
            statement.reset()
            statement.bind(1, attachmentId)
            statement.bindText(2, it)
            statement.step()
        }
        diaryDatabase.commit()
        statement.release()

        return attachmentId
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // no file picked
        data ?: return

        when (requestCode) {
            RequestCode.START_ACTIVITY_0 -> {
                // pick file from the file library
                val fileInfo = data.getParcelableExtra("fileInfo") as FileInfo
                val filePreviewView = FileLibraryActivity.getFilePreviewView(this, fileInfo)
                filePreviewView.background = ContextCompat.getDrawable(this, R.drawable.view_stroke)
                fileListLL.addView(filePreviewView)
                fileIdentifierList.add(fileInfo.identifier)
            }
            else -> {

            }
        }
    }

    companion object {
        /**
         * intent long extra
         */
        const val EXTRA_ATTACHMENT_ID = "attachmentId"
    }
}