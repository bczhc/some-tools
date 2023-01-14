package pers.zhc.tools.transfer

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ScrollView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.TransferSendActivityBinding
import pers.zhc.tools.databinding.TransferTarProgressViewBinding
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.jni.JNI.Transfer.ReceiveProgressCallback
import pers.zhc.tools.utils.*
import pers.zhc.tools.views.SmartHintEditText
import pers.zhc.tools.views.WrapLayout
import java.io.File

/**
 * @author bczhc
 */
class TransferSendActivity : BaseActivity() {
    private lateinit var containerLayout: WrapLayout
    private lateinit var addressET: EditText

    private val launchers = object {
        val filePicker = FilePicker.getLauncher(this@TransferSendActivity) { path ->
            path ?: return@getLauncher
            when (val child = containerLayout.getChildAt(0)) {
                is SmartHintEditText -> {
                    // file path
                    child.editText.setText(path)
                }
                is EditText -> {
                    // text
                    child.setText(File(path).readText())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = TransferSendActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val intent = intent
        // activity is launched from "Open as" dialog
        var initFilePath = if (intent.action == Intent.ACTION_VIEW) {
            // TODO: workaround; should use content provider but not direct path
            intent.data.nullMap { it.path }
        } else null

        addressET = bindings.destinationAddressEt.editText
        val typeSpinner = bindings.typeSpinner
        val pickFileButton = bindings.pickFileBtn
        containerLayout = bindings.containerLayout
        val sendButton = bindings.sendBtn
        val qrCodeButton = bindings.qrCodeBtn

        val typeStrings = resources.getStringArray(R.array.transfer_types)
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, typeStrings)

        typeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0 || position == 1) {
                    val inflate = View.inflate(this@TransferSendActivity, R.layout.transfer_path_et, null).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    }
                    containerLayout.setView(inflate)

                    if (inflate is SmartHintEditText) {
                        inflate.editText.setText(initFilePath)
                        // only set once
                        initFilePath = null
                    } else unreachable()
                } else {
                    val et = EditText(this@TransferSendActivity).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                        gravity = Gravity.START or Gravity.TOP
                        hint = getString(R.string.text)
                    }
                    containerLayout.setView(et)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        pickFileButton.setOnClickListener {
            when (typeSpinner.selectedItemPosition) {
                0 -> {
                    // file
                    launchers.filePicker.launch(FilePicker.PICK_FILE)
                }
                1 -> {
                    // folder
                    launchers.filePicker.launch(FilePicker.PICK_FOLDER)
                }
                2 -> {
                    // text
                    launchers.filePicker.launch(FilePicker.PICK_FILE)
                }
            }
        }

        sendButton.setOnClickListener {
            val address = addressET.text.toString()

            val mark = when (typeSpinner.selectedItemPosition) {
                0 -> Mark.FILE
                1 -> Mark.TAR
                2 -> Mark.TEXT
                else -> unreachable()
            }
            val file = if (mark == Mark.TEXT) {
                val text = (containerLayout.getChildAt(0) as EditText).text.toString()
                saveTempText(text).also { pers.zhc.util.Assertion.doAssertion(it.exists()) }
            } else {
                File((containerLayout.getChildAt(0) as SmartHintEditText).text.toString())
            }

            if (!file.exists()) {
                ToastUtils.show(this, R.string.file_not_exist)
                return@setOnClickListener
            }

            showSendingDialog(mark, address, file)
        }

        qrCodeButton.setOnClickListener {
            handleScanQrCode()
        }
    }

    private fun showSendingDialog(mark: Mark, address: String, file: File) {
        val pair = when (mark) {
            Mark.FILE -> {
                val progressDialog = ProgressDialog(this)
                val progressView = progressDialog.getProgressView()
                progressView.apply {
                    setIsIndeterminateMode(false)
                }
                val tryDo = AsyncTryDo()
                val callback = object : ReceiveProgressCallback() {
                    override fun fileProgress(progress: Float) {
                        tryDo.tryDo { _, notifier ->
                            runOnUiThread {
                                progressView.setProgressAndText(progress)
                                notifier.finish()
                            }
                        }
                    }
                }
                Pair(progressDialog, callback)
            }
            Mark.TEXT -> {
                val progressDialog = ProgressDialog(this)
                val progressView = progressDialog.getProgressView()
                progressView.apply {
                    setIsIndeterminateMode(true)
                }
                val callback = object : ReceiveProgressCallback() {}
                Pair(progressDialog, callback)
            }
            Mark.TAR -> {
                val dialog = Dialog(this)
                val bindings = TransferTarProgressViewBinding.inflate(layoutInflater)
                val inflate = bindings.root.apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
                dialog.setContentView(inflate)

                val textView = bindings.tarFilesTv
                val scrollView = bindings.scrollView

                val callback = object : ReceiveProgressCallback() {
                    override fun tarProgress(logLine: String?) {
                        runOnUiThread {
                            textView.append(logLine)
                            textView.append("\n")
                            scrollView.scrollToBottom()
                        }
                    }
                }
                Pair(dialog, callback)
            }
        }
        val dialog = pair.first
        val progressCallback = pair.second

        dialog.show()

        Thread {
            try {
                JNI.Transfer.send(address, mark.enumInt, file.path, progressCallback)
                runOnUiThread {
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                ToastUtils.showException(this, e)
                runOnUiThread {
                    dialog.dismiss()
                }
            }
        }.start()
    }

    private fun saveTempText(text: String): File {
        return File(cacheDir, "temp-${System.currentTimeMillis()}.txt").also { it.writeText(text) }
    }

    fun ScrollView.scrollToBottom() {
        this.fullScroll(View.FOCUS_DOWN)
    }

    private val socketIpv4AddrPattern = Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+:\\d{1,5}$")

    private val qrCodeLauncher = registerForActivityResult(ScanContract()) {
        it ?: return@registerForActivityResult
        val contents = it.contents ?: return@registerForActivityResult
        if (contents.matches(socketIpv4AddrPattern)) {
            addressET.setText(contents)
        } else {
            ToastUtils.show(this, R.string.transfer_invalid_address_toast)
        }
    }

    private fun handleScanQrCode() {
        qrCodeLauncher.launch(ScanOptions().apply {
            setOrientationLocked(false)
            setBeepEnabled(false)
        })
    }
}
