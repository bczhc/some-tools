package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.diary_attachment_file_library_file_detail_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R

/**
 * @author bczhc
 */
class FileLibraryFileDetailActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_file_library_file_detail_activity)

        val ll = ll!!
        val browserFileBtn = browser_file_btn!!

        val intent = intent
        val fileInfo = intent.getSerializableExtra("fileInfo") as FileLibraryActivity.FileInfo

        val filePreviewView = FileLibraryActivity.getFilePreviewView(this, fileInfo)
        ll.addView(filePreviewView, 0)

        browserFileBtn.setOnClickListener {
            startActivity(Intent(this, FileBrowserActivity::class.java))
        }
    }
}