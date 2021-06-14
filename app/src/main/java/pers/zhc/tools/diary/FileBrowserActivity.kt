package pers.zhc.tools.diary

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import pers.zhc.tools.R
import pers.zhc.tools.diary.FileLibraryActivity.Companion.getTextContent
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.views.ScalableImageView
import java.io.File

/**
 * @author bczhc
 */
class FileBrowserActivity : DiaryBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val fileInfo = intent.getParcelableExtra<FileInfo>(EXTRA_FILE_INFO)!!

        when (StorageType.get(fileInfo.storageTypeEnumInt)) {
            StorageType.TEXT -> {
                val tv = getTextFileContentView(getTextContent(diaryDatabase, fileInfo.identifier))
                setContentView(tv)
            }
            StorageType.RAW -> {
                getRawFileContentView(File(fileInfo.filename!!))
            }
            StorageType.IMAGE -> {
                val imageFileContentView = getImageFileContentView(
                    File(
                        DiaryAttachmentSettingsActivity.getFileStoragePath(diaryDatabase),
                        fileInfo.identifier
                    ).path
                )
                setContentView(imageFileContentView)
            }
            else -> {
                // TODO: 4/25/21 for else file formats
            }
        }
    }

    private fun getImageFileContentView(filepath: String): View {
        val bitmap = BitmapFactory.decodeFile(filepath)
        if (bitmap == null) {
            ToastUtils.show(this, R.string.diary_file_broser_open_file_failed)
            return View(this)
        }
        return ScalableImageView(this).apply {
            setBitmap(bitmap)
        }
    }

    private fun getRawFileContentView(file: File) {
        TODO("Not yet implemented")
    }

    private fun getTextFileContentView(content: String): ScrollView {
        val sv = ScrollView(this)
        sv.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val tv = TextView(this)
        tv.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        tv.setTextIsSelectable(true)
        sv.addView(tv)

        tv.textSize = DiaryContentPreviewActivity.getEditTextTextSize(this)
        tv.text = content
        return sv
    }

    companion object {
        /**
         * intent parcelable extra
         */
        const val EXTRA_FILE_INFO = "fileInfo"
    }
}