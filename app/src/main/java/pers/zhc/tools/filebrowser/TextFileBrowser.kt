package pers.zhc.tools.filebrowser

import android.os.Bundle
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.DisplayUtil
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class TextFileBrowser : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val filePath = intent.getStringExtra(EXTRA_PATH) ?: throw Exception("file path missing")
        val charset = intent.getStringExtra(EXTRA_CHARSET) ?: StandardCharsets.UTF_8.name()!!

        val content = File(filePath).readText(Charset.forName(charset))

        setContentView(ScrollView(this).apply {
            addView(TextView(this@TextFileBrowser).apply {
                text = content
                textSize = DisplayUtil.getDefaultEditTextTextSize(this@TextFileBrowser)
                setTextIsSelectable(true)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setTextColor(ActivityCompat.getColor(this@TextFileBrowser, R.color.highContrastTextColor))
            })
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        })
    }

    companion object {
        /**
         * intent string extra
         */
        const val EXTRA_PATH = "path"

        /**
         * intent string extra
         */
        const val EXTRA_CHARSET = "charset"
    }
}