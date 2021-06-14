package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.diary_attachment_file_library_file_detail_activity.*
import pers.zhc.tools.R
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
        val identifier = intent.getStringExtra(EXTRA_IDENTIFIER)!!

        val fileInfo = FileLibraryActivity.getFileInfo(diaryDatabase, identifier)

        val filePreviewView = FileLibraryActivity.getFilePreviewView(this, fileInfo, null)
        ll.addView(filePreviewView, 0)

        browserFileBtn.setOnClickListener {
            if (fileInfo.storageTypeEnumInt != StorageType.TEXT.enumInt) {
                val path = File(DiaryAttachmentSettingsActivity.getFileStoragePath(diaryDatabase), identifier).path
                if (!File(path).exists()) {
                    FileLibraryActivity.showFileNotExistDialog(this, diaryDatabase, identifier)
                    return@setOnClickListener
                }
            }
            val i = Intent(this, FileBrowserActivity::class.java)
            i.putExtra(FileBrowserActivity.EXTRA_FILE_INFO, fileInfo)
            startActivity(i)
        }
    }

    companion object {
        /**
         * intent string extra
         */
        const val EXTRA_IDENTIFIER = "identifier"
    }
}