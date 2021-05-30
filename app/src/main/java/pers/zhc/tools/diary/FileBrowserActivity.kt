package pers.zhc.tools.diary

import android.os.Bundle
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.utils.Common
import java.io.File

/**
 * @author bczhc
 */
class FileBrowserActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val fileInfo = intent.getSerializableExtra(EXTRA_FILE_INFO)!! as FileInfo

        when (StorageType.get(fileInfo.storageTypeEnumInt)) {
            StorageType.TEXT -> {
                val tv = getTextFileContentView(fileInfo.content)
                setContentView(tv)
            }
            StorageType.RAW -> {
                getRawFileContentView(File(fileInfo.content))
            }
            else -> {
                // TODO: 4/25/21 for else file formats
            }
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
         * intent serializable extra
         */
        const val EXTRA_FILE_INFO = "fileInfo"
    }
}