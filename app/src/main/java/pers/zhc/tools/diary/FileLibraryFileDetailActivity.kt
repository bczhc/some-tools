package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.diary_file_library_file_detail_activity.*
import pers.zhc.tools.R
import pers.zhc.tools.diary.fragments.FileLibraryFragment
import pers.zhc.tools.filebrowser.AudioPlayerActivity
import pers.zhc.tools.filebrowser.ImageFileBrowser
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

        val fileInfo = diaryDatabase.queryAttachmentFile(identifier)!!

        val filePreviewView = FileLibraryFragment.getFilePreviewView(this, fileInfo, null)
        container.addView(filePreviewView)

        val fileStoragePath by lazy {
            diaryDatabase.queryExtraInfo()!!.diaryAttachmentFileLibraryStoragePath!!
        }

        browserFileBtn.setOnClickListener {

            when (fileInfo.storageType) {
                StorageType.RAW -> TODO()
                StorageType.TEXT -> {
                    startActivity(Intent(this, TextBrowserActivity::class.java).apply {
                        putExtra(TextBrowserActivity.EXTRA_IDENTIFIER, fileInfo.identifier)
                    })
                }

                StorageType.IMAGE -> {
                    val filePath = File(fileStoragePath, fileInfo.identifier).path

                    startActivity(Intent(this, ImageFileBrowser::class.java).apply {
                        putExtra(ImageFileBrowser.EXTRA_PATH, filePath)
                    })
                }

                StorageType.AUDIO -> {
                    val filePath = File(fileStoragePath, fileInfo.identifier).path

                    startActivity(Intent(this, AudioPlayerActivity::class.java).apply {
                        putExtra(AudioPlayerActivity.EXTRA_FILE_PATH, filePath)
                    })
                }
            }
        }
    }

    companion object {
        /**
         * intent string extra
         */
        const val EXTRA_IDENTIFIER = "identifier"
    }
}