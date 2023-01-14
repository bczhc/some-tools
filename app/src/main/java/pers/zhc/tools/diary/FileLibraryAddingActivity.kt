package pers.zhc.tools.diary

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContract
import pers.zhc.tools.R
import pers.zhc.tools.databinding.DiaryFileLibraryAddingFileActivityBinding
import pers.zhc.tools.databinding.DiaryFileLibraryAddingFileActivityFileModeBinding
import pers.zhc.tools.databinding.DiaryFileLibraryAddingFileActivityTextModeBinding
import pers.zhc.tools.filepicker.FilePickerActivityContract
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.utils.FileUtil
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.views.SmartHintEditText
import java.io.File

/**
 * @author bczhc
 */
class FileLibraryAddingActivity : DiaryBaseActivity() {
    private lateinit var topDynamicLL: LinearLayout
    private lateinit var descriptionET: EditText
    private lateinit var spinner: Spinner
    private lateinit var currentStorageType: StorageType
    private var text: String? = null
    private var resultIdentifier: String? = null

    private val launchers = object {
        val pickFile = registerForActivityResult(
            FilePickerActivityContract(
                FilePickerActivityContract.FilePickerType.PICK_FILE,
                false
            )
        ) { result ->
            result ?: return@registerForActivityResult
            val pickedFile = result.path
            topDynamicLL.findViewById<SmartHintEditText>(R.id.picked_file_et)?.editText?.setText(pickedFile)
        }
        val editTextAttachment =
            registerForActivityResult(DiaryFileLibraryEditTextActivity.TextEditContract()) { result ->
                this@FileLibraryAddingActivity.text = result
                topDynamicLL.findViewById<TextView>(R.id.length_tv)?.text = getString(R.string.diary_file_library_edit_text_length_tv, text?.length)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = DiaryFileLibraryAddingFileActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        descriptionET = bindings.descriptionEt
        val submitBtn = bindings.submitBtn
        spinner = bindings.spinner
        topDynamicLL = bindings.topDynamicLl

        addSpinner()
        submitBtn.setOnClickListener { submit() }
    }

    private fun submit() {
        if (currentStorageType == StorageType.TEXT) {
            syncSubmitText(if (this.text == null) "" else this.text!!)
        } else {
            val pickedFileET = topDynamicLL.findViewById<SmartHintEditText>(R.id.picked_file_et)!!.editText
            syncSubmitFile(File(pickedFileET.text.toString()))
        }
    }

    private fun syncSubmitFile(file: File) {
        val progressView = View.inflate(this, R.layout.diary_file_library_add_progress_view, null)
        val msgTV = progressView.findViewById<TextView>(R.id.msg_tv)

        if (!file.exists()) {
            ToastUtils.show(this, R.string.diary_file_not_exist_alert_msg)
            return
        }

        val dialog = Dialog(this)
        DialogUtil.setDialogAttr(
            dialog,
            false,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        )
        dialog.setContentView(progressView)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()

        msgTV.setText(R.string.calculating_file_digest)

        Thread {
            val identifier = computeIdentifier(file)
            runOnUiThread { msgTV.setText(R.string.insert_record) }

            val filename = file.name
            val storageType = currentStorageType
            val description = descriptionET.text.toString()

            if (diaryDatabase.hasFileRecord(identifier)) {
                runOnUiThread {
                    DialogUtil.createConfirmationAlertDialog(this, { _, _ ->
                        diaryDatabase.updateFileRecord(identifier, filename, storageType, description)
                        runOnUiThread { dialog.dismiss() }
                        ToastUtils.show(this, R.string.updating_done)
                        resultIdentifier = identifier
                    }, { _, _ ->
                        runOnUiThread { dialog.dismiss() }
                    }, R.string.file_exists_alert_msg).show()
                }
            } else {
                diaryDatabase.insertFileRecord(
                    FileInfo(
                        filename,
                        System.currentTimeMillis(),
                        storageType,
                        description,
                        identifier
                    )
                )

                runOnUiThread { msgTV.setText(R.string.copying_file) }
                FileUtil.copy(
                    file,
                    File(LocalInfo.attachmentStoragePath, identifier)
                )

                runOnUiThread {
                    msgTV.setText(R.string.done)
                    dialog.dismiss()
                    resultIdentifier = identifier
                    ToastUtils.show(this, R.string.adding_done)
                    finish()
                }
            }
        }.start()
    }

    private fun syncSubmitText(text: String) {
        val progressView = View.inflate(this, R.layout.diary_file_library_add_progress_view, null)
        progressView.findViewById<TextView>(R.id.msg_tv).setText(R.string.diary_file_library_text_adding_msg)

        val description = descriptionET.text.toString()

        val dialog = Dialog(this)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.setContentView(progressView)
        dialog.show()

        Thread {
            val identifier = computeIdentifier(text)
            diaryDatabase.insertTextRecord(identifier, text, description)
            dialog.cancel()
            resultIdentifier = identifier
            runOnUiThread {
                ToastUtils.show(this, R.string.adding_done)
                finish()
            }
        }.start()
    }

    private fun addSpinner() {
        val storageTypeValues = StorageType.values()
        val arrayAdapter =
            object : ArrayAdapter<StorageType>(
                this,
                android.R.layout.simple_list_item_1,
                storageTypeValues
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    setMyView(position, view)
                    return view
                }

                private fun setMyView(position: Int, view: View) {
                    val item = getItem(position)!!
                    view.findViewById<TextView>(android.R.id.text1).setText(item.textResInt)
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val dropDownView = super.getDropDownView(position, convertView, parent)
                    setMyView(position, dropDownView)
                    return dropDownView
                }
            }
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1)

        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentStorageType = storageTypeValues[position]

                changeTopView(currentStorageType)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        spinner.setSelection(0)
    }

    private fun changeTopView(storageType: StorageType) {
        val layoutInflater = LayoutInflater.from(this)

        val view = if (storageType == StorageType.TEXT) {
            DiaryFileLibraryAddingFileActivityTextModeBinding.inflate(layoutInflater).also {
                it.enterTextBtn.setOnClickListener {
                    launchers.editTextAttachment.launch(this.text)
                }
            }.root
        } else {
            DiaryFileLibraryAddingFileActivityFileModeBinding.inflate(layoutInflater).also {
                it.pickFileBtn.setOnClickListener {
                    launchers.pickFile.launch(Unit)
                }
            }.root
        }
        topDynamicLL.removeAllViews()
        topDynamicLL.addView(view)
    }

    override fun finish() {
        if (resultIdentifier != null) {
            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_RESULT_IDENTIFIER, resultIdentifier)
            setResult(0, resultIntent)
        }
        super.finish()
    }

    companion object {
        /**
         * result string intent
         */
        const val EXTRA_RESULT_IDENTIFIER = "resultIdentifier"
    }

    class AddFileContract : ActivityResultContract<Unit, AddFileContract.Result?>() {
        class Result(
            val identifier: String
        )

        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(context, FileLibraryAddingActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Result? {
            intent ?: return null
            return Result(intent.getStringExtra(EXTRA_RESULT_IDENTIFIER)!!)
        }
    }
}