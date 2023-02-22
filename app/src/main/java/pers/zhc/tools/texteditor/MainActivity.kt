package pers.zhc.tools.texteditor

import android.content.Intent
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.TextEditorMainBinding
import pers.zhc.tools.utils.nullMap
import java.io.File

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = TextEditorMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val editText = bindings.editText
        editText.setZoomFontSizeEnabled(true)

        // activity is launched from "Open as" dialog
        val initFilePath = if (intent.action == Intent.ACTION_VIEW) {
            // TODO: workaround; should use content provider but not direct path
            intent.data.nullMap { it.path }
        } else null

        val text = initFilePath.nullMap {
            File(it).readText()
        } ?: ""

        editText.setText(text)
    }
}