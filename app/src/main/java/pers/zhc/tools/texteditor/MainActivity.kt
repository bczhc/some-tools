package pers.zhc.tools.texteditor

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.addCallback
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.TextEditorMainBinding
import pers.zhc.tools.jni.JNI.Encoding
import pers.zhc.tools.jni.JNI.Encoding.EncodingVariant
import pers.zhc.tools.utils.*
import java.io.File

class MainActivity : BaseActivity() {
    private var isModified = false
    private lateinit var editText: EditText
    private var tmpFile: File? = null
    private lateinit var openAsResult: OpenAsResult

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = TextEditorMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val editText = bindings.editText
        editText.setZoomFontSizeEnabled(true)
        this.editText = editText.editText

        // activity is launched from "Open as" dialog
        val openAsResult = checkFromOpenAs()
        if (openAsResult == null) {
            ToastUtils.show(this, R.string.file_path_please_use_open_as_hint)
            finish()
            return
        }
        this.openAsResult = openAsResult

        val inputStream = openAsResult.openInputStream()
        if (inputStream == null) {
            ToastUtils.show(this, R.string.file_open_failed_toast)
            finish()
            return
        }

        val tmpFile = tmpFile()
        this.tmpFile = tmpFile
        tmpFile.copyFrom(inputStream)

        this.editText.setText(Encoding.readFile(tmpFile.path, EncodingVariant.UTF_8))

        this.editText.doAfterTextChanged {
            isModified = true
        }

        onBackPressedDispatcher.addCallback {
            if (isModified) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        save()
                        ToastUtils.show(this@MainActivity, R.string.save_success_toast)
                        finish()
                    }
                    .setNegativeButton(R.string.no) { _, _ ->
                        finish()
                    }
                    .setTitle(R.string.whether_to_save)
                    .show()
            } else finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.text_file_browser_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val update = { encoding: EncodingVariant ->
            editText.setText(Encoding.readFile(tmpFile!!.path, encoding))
        }
        when (item.itemId) {
            R.id.utf8 -> {
                item.isChecked = true
                update(EncodingVariant.UTF_8)
            }

            R.id.utf16le -> {
                item.isChecked = true
                update(EncodingVariant.UTF_16LE)
            }

            R.id.utf16be -> {
                item.isChecked = true
                update(EncodingVariant.UTF_16BE)
            }

            R.id.utf32le -> {
                item.isChecked = true
                update(EncodingVariant.UTF_32LE)
            }

            R.id.utf32be -> {
                item.isChecked = true
                update(EncodingVariant.UTF_32BE)
            }

            R.id.gbk -> {
                item.isChecked = true
                update(EncodingVariant.GBK)
            }

            R.id.gb18030 -> {
                item.isChecked = true
                update(EncodingVariant.GB18030)
            }

            else -> {
                return false
            }
        }
        return true
    }

    private fun save() {
        val text = editText.text.toString()
        val stream = this.openAsResult.openOutputStream("w")
        if (stream == null) {
            ToastUtils.show(this, R.string.saving_failed)
            return
        }
        stream.writer().also { it.write(text) }.flush()
        stream.close()
    }
}
