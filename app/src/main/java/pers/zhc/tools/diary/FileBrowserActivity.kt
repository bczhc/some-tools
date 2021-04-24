package pers.zhc.tools.diary

import android.os.Bundle
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.diary.FileLibraryActivity.StorageType
import pers.zhc.tools.utils.Common
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

/**
 * @author bczhc
 */
class FileBrowserActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val storageTypeEnumInt = intent.getIntExtra("storageType", -1)
        Common.doAssertion(storageTypeEnumInt != -1)
        val storageType = StorageType.get(storageTypeEnumInt)

        val filePath = intent.getStringExtra("filePath")!!
        val file = File(filePath)
        Common.doAssertion(file.exists())

        when (storageType) {
            StorageType.TEXT -> {
                val tv = getTextFileContentView(file)
                setContentView(tv)
            }
            StorageType.RAW -> {
                getRawFileContentView(file)
            }
            else -> {
                // TODO: 4/25/21 for else file formats
            }
        }
    }

    private fun getRawFileContentView(file: File) {
        TODO("Not yet implemented")
    }

    private fun getTextFileContentView(file: File): ScrollView {
        val sb = StringBuilder()

        val fis = FileInputStream(file)
        val isr = InputStreamReader(fis)
        val br = BufferedReader(isr)
        while (true) {
            val read = br.readLine()
            read ?: break
            sb.appendLine(read)
        }
        br.close()
        isr.close()
        fis.close()

        val sv = ScrollView(this)
        sv.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val tv = TextView(this)
        tv.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        sv.addView(tv)

        tv.textSize = DiaryContentPreviewActivity.getEditTextTextSize(this)
        tv.text = sb.toString()
        return sv
    }
}