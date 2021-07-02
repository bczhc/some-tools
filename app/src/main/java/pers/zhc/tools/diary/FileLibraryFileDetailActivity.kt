package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.diary_file_library_file_detail_activity.*
import pers.zhc.tools.R
import pers.zhc.tools.diary.fragments.FileLibraryFragment
import java.io.File

/**
 * @author bczhc
 */
class FileLibraryFileDetailActivity : DiaryBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_file_library_file_detail_activity)

        val container = container!!
        val browserFileBtn = browser_file_btn!!

        val intent = intent
        val identifier = intent.getStringExtra(EXTRA_IDENTIFIER)!!

        val fileInfo = FileLibraryFragment.getFileInfo(diaryDatabase, identifier)

        val filePreviewView = FileLibraryFragment.getFilePreviewView(this, fileInfo, null)
        container.addView(filePreviewView)

        browserFileBtn.setOnClickListener {
            if (fileInfo.storageTypeEnumInt != StorageType.TEXT.enumInt) {
                val path = File(DiaryAttachmentSettingsActivity.getFileStoragePath(diaryDatabase), identifier).path
                if (!File(path).exists()) {
                    FileLibraryFragment.showFileNotExistDialog(this, diaryDatabase, identifier)
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