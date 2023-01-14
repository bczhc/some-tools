package pers.zhc.tools.diary

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.ContextCompat
import pers.zhc.tools.R
import pers.zhc.tools.databinding.DiaryAttachmentAddingActivityBinding
import pers.zhc.tools.diary.fragments.FileLibraryFragment
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.getLongExtraOrNull
import pers.zhc.tools.utils.stepBind

class DiaryAttachmentAddingActivity : DiaryBaseActivity() {
    private lateinit var descriptionET: EditText
    private lateinit var titleET: EditText
    private val fileIdentifierList = ArrayList<String>()
    private lateinit var fileListLL: LinearLayout

    private val launchers = object {
        val pickFile = registerForActivityResult(FileLibraryActivity.PickFileContract()) { result ->
            result ?: return@registerForActivityResult
            val identifier = result.identifier
            if (fileIdentifierList.contains(identifier)) {
                ToastUtils.show(this@DiaryAttachmentAddingActivity, R.string.diary_attachment_library_duplicate_toast)
                return@registerForActivityResult
            }
            val filePreviewView =
                FileLibraryFragment.getFilePreviewView(this@DiaryAttachmentAddingActivity, diaryDatabase, identifier)
            filePreviewView.background =
                ContextCompat.getDrawable(this@DiaryAttachmentAddingActivity, R.drawable.view_stroke)
            fileListLL.addView(filePreviewView)
            fileIdentifierList.add(identifier)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = DiaryAttachmentAddingActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)
        titleET = bindings.titleEt
        fileListLL = bindings.fileListLl
        descriptionET = bindings.descriptionEt
        val createAttachmentBtn = bindings.createAttachmentBtn
        val pickFileBtn = bindings.pickFileBtn

        pickFileBtn.setOnClickListener {
            launchers.pickFile.launch(Unit)
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

    companion object {
        /**
         * intent string extra
         * the id of the added attachment
         */
        const val EXTRA_RESULT_ATTACHMENT_ID = "resultAttachmentId"
    }

    class AddAttachmentContract : ActivityResultContract<Unit, AddAttachmentContract.Result?>() {
        class Result(
            val attachmentId: Long
        )

        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(context, DiaryAttachmentAddingActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Result? {
            intent ?: return null
            return Result(intent.getLongExtraOrNull(EXTRA_RESULT_ATTACHMENT_ID)!!)
        }
    }
}