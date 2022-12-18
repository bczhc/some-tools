package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.diary_attachment_preview_activity.*
import pers.zhc.tools.R
import pers.zhc.tools.diary.fragments.FileLibraryFragment
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

        val attachment = diaryDatabase.queryAttachment(attachmentId)

        titleTV.text =
            getString(R.string.diary_attachment_preview_activity_title_is, attachment.title)
        descriptionTV.text = getString(
            R.string.diary_attachment_preview_activity_description_is,
            attachment.description
        )

        // TODO: use RecyclerView
        val attachmentFiles = diaryDatabase.queryAttachmentFiles(attachmentId)
        for (fileInfo in attachmentFiles) {
            val content = if (fileInfo.storageType == StorageType.TEXT) {
                // TODO: lazily query text attachments
                diaryDatabase.queryTextAttachment(fileInfo.identifier)
            } else {
                null
            }
            val filePreviewView = FileLibraryFragment.getFilePreviewView(this, fileInfo, content)
            filePreviewView.setOnClickListener {
                val startIntent = Intent(this, FileLibraryFileDetailActivity::class.java)
                startIntent.putExtra(FileLibraryFileDetailActivity.EXTRA_IDENTIFIER, fileInfo.identifier)
                startActivity(startIntent)
            }
            fileListLL.addView(filePreviewView)
        }
    }

    companion object {
        /**
         * long intent extra
         */
        const val EXTRA_ATTACHMENT_ID = "attachmentId"
    }
}