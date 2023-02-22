package pers.zhc.tools.texteditor

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.TextEditorMainBinding
import pers.zhc.tools.utils.DialogUtils
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.nullMap
import java.io.File

class MainActivity : BaseActivity() {
    private var isModified = false
    private lateinit var editText: EditText
    private var file: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = TextEditorMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val editText = bindings.editText
        editText.setZoomFontSizeEnabled(true)
        this.editText = editText.editText

        // activity is launched from "Open as" dialog
        val initFilePath = if (intent.action == Intent.ACTION_VIEW) {
            // TODO: workaround; should use content provider but not direct path
            intent.data.nullMap { it.path }
        } else null
        this.file = initFilePath.nullMap { File(it) }

        val text = initFilePath.nullMap {
            File(it).readText()
        } ?: ""

        editText.setText(text)

        this.editText.doAfterTextChanged {
            isModified = true
        }
    }

    private fun save() {
        val text = editText.text.toString()
        this.file!!.writeText(text)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isModified && file != null) {
            DialogUtils.createConfirmationAlertDialog(
                this,
                positiveAction = { _, _ ->
                    save()
                    ToastUtils.show(this, R.string.save_success_toast)
                    super.onBackPressed()
                },
                titleRes = R.string.whether_to_save
            ).show()
        } else super.onBackPressed()
    }
}