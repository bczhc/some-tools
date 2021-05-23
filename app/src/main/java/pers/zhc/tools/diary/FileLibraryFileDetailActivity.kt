package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.diary_attachment_file_library_file_detail_activity.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.DialogUtil
import java.io.File

/**
 * @author bczhc
 */
class FileLibraryFileDetailActivity : DiaryBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_file_library_file_detail_activity)

        val ll = ll!!
        val browserFileBtn = browser_file_btn!!

        val intent = intent
        val identifier = intent.getStringExtra(EXTRA_FILE_IDENTIFIER)!!
        val statement =
            diaryDatabase.compileStatement("SELECT * FROM diary_attachment_file WHERE identifier IS ?")
        statement.bindText(1, identifier)
        val cursor = statement.cursor
        Common.doAssertion(cursor.step())
        val storageType = cursor.getInt(statement.getIndexByColumnName("storage_type"))
        val filename = cursor.getText(statement.getIndexByColumnName("filename"))
        val description = cursor.getText(statement.getIndexByColumnName("description"))
        val additionTimestamp = cursor.getLong(statement.getIndexByColumnName("addition_timestamp"))
        statement.release()

        val filePreviewView = FileLibraryActivity.getFilePreviewView(
            this, FileLibraryActivity.FileInfo(
                filename,
                additionTimestamp,
                storageType,
                description,
                identifier
            )
        )
        ll.addView(filePreviewView, 0)

        browserFileBtn.setOnClickListener {
            val path = File(DiaryAttachmentSettingsActivity.getFileStoragePath(diaryDatabase), identifier).path
            if (!File(path).exists()) {
                FileLibraryActivity.showFileNotExistDialog(this, diaryDatabase, identifier)
                return@setOnClickListener
            }

            val i = Intent(this, FileBrowserActivity::class.java)
            i.putExtra("storageType", storageType)
            i.putExtra("filePath", path)
            startActivity(i)
        }
    }

    companion object {
        /**
         * intent string extra
         */
        const val EXTRA_FILE_IDENTIFIER = "fileIdentifier"
    }
}