package pers.zhc.tools.diary

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.diary_attachment_file_library_adding_file_activity.*
import kotlinx.android.synthetic.main.diary_attachment_file_library_adding_file_activity_file_mode.view.*
import kotlinx.android.synthetic.main.diary_attachment_file_library_adding_file_activity_text_mode.view.*
import kotlinx.android.synthetic.main.diary_file_library_add_progress_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.utils.FileUtil
import pers.zhc.tools.utils.ToastUtils
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
    private lateinit var resultIdentifier: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_file_library_adding_file_activity)

        descriptionET = description_et!!
        val submitBtn = submit_btn!!
        spinner = findViewById(R.id.spinner)!!
        topDynamicLL = top_dynamic_ll!!

        addSpinner()
        submitBtn.setOnClickListener { submit() }
    }

    private fun submit() {
        if (currentStorageType == StorageType.TEXT) {
            syncSubmitText(if (this.text == null) "" else this.text!!)
        } else {
            val pickedFileET = topDynamicLL.picked_file_et!!.editText
            syncSubmitFiles(File(pickedFileET.text.toString()))
        }
    }

    private fun syncSubmitFiles(file: File) {
        val progressView = View.inflate(this, R.layout.diary_file_library_add_progress_view, null)
        val msgTV = progressView.msg_tv

        if (!file.exists()) {
            ToastUtils.show(this, R.string.file_not_exist_alert_msg)
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
            val identifier = DiaryMainActivity.computeIdentifier(file)
            runOnUiThread { msgTV.setText(R.string.insert_record) }

            val filename = file.name
            val storageTypeEnumInt = currentStorageType.enumInt
            val description = descriptionET.text.toString()

            if (hasRecord(identifier)) {
                runOnUiThread {
                    DialogUtil.createConfirmationAlertDialog(this, { _, _ ->
                        updateFileRecord(storageTypeEnumInt, filename, description, identifier)
                        runOnUiThread { dialog.dismiss() }
                        ToastUtils.show(this, R.string.updating_done)
                        resultIdentifier = identifier
                    }, { _, _ ->
                        runOnUiThread { dialog.dismiss() }
                    }, R.string.file_exists_alert_msg).show()
                }
            } else {
                insertDatabase(identifier, filename, storageTypeEnumInt, description)

                runOnUiThread { msgTV.setText(R.string.copying_file) }
                FileUtil.copy(
                    file,
                    File(DiaryAttachmentSettingsActivity.getFileStoragePath(diaryDatabase)!!, identifier)
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
        progressView.msg_tv.setText(R.string.diary_file_library_text_adding_msg)

        val description = descriptionET.text.toString()

        val dialog = Dialog(this)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.setContentView(progressView)
        dialog.show()

        Thread {
            val identifier = DiaryMainActivity.computeIdentifier(text)
            insertDatabase(identifier, text, StorageType.TEXT.enumInt, description)
            dialog.cancel()
            resultIdentifier = identifier
            runOnUiThread {
                ToastUtils.show(this, R.string.adding_done)
                finish()
            }
        }.start()
    }

    private fun insertDatabase(
        identifier: String,
        addition_timestamp: Long,
        content: String,
        storageTypeInt: Int,
        description: String
    ) {
        val statement =
            diaryDatabase.compileStatement("INSERT INTO diary_attachment_file(identifier, addition_timestamp, content, storage_type, description)\nVALUES (?, ?, ?, ?, ?)")
        statement.bind(arrayOf(identifier, addition_timestamp, content, storageTypeInt, description))
        statement.step()
        statement.release()
    }

    private fun insertDatabase(identifier: String, content: String, storageTypeInt: Int, description: String) {
        insertDatabase(identifier, System.currentTimeMillis(), content, storageTypeInt, description)
    }

    private fun hasRecord(identifier: String): Boolean {
        return diaryDatabase.hasRecord("SELECT * FROM diary_attachment_file WHERE identifier IS '$identifier'")
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
        val view = if (storageType == StorageType.TEXT) {
            val inflate =
                View.inflate(this, R.layout.diary_attachment_file_library_adding_file_activity_text_mode, null)
            inflate.enter_text_btn.setOnClickListener {
                val intent = Intent(this, DiaryFileLibraryEditTextActivity::class.java)
                intent.putExtra(DiaryFileLibraryEditTextActivity.EXTRA_INITIAL_TEXT, this.text)
                startActivityForResult(intent, RequestCode.START_ACTIVITY_1)
            }
            inflate
        } else {
            val inflate =
                View.inflate(this, R.layout.diary_attachment_file_library_adding_file_activity_file_mode, null)
            inflate.pick_file_btn.setOnClickListener {
                val intent = Intent(this, FilePicker::class.java)
                intent.putExtra("option", FilePicker.PICK_FILE)
                startActivityForResult(intent, RequestCode.START_ACTIVITY_0)
            }
            inflate
        }
        topDynamicLL.removeAllViews()
        topDynamicLL.addView(view)
    }

    private fun updateFileRecord(
        storageTypeOrdinal: Int,
        filename: String,
        description: String,
        identifier: String,
    ) {
        val statement =
            diaryDatabase.compileStatement("UPDATE diary_attachment_file\nSET filename     = ?,\n    storage_type = $storageTypeOrdinal,\n    description  = ?\nWHERE identifier IS ?")
        statement.reset()
        statement.bindText(1, filename)
        statement.bindText(2, description)
        statement.bindText(3, identifier)
        statement.step()
        statement.release()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCode.START_ACTIVITY_0 -> {
                // pick file
                data!!
                val pickedFile = data.getStringExtra("result") ?: return
                topDynamicLL.picked_file_et?.editText?.setText(pickedFile)
            }
            RequestCode.START_ACTIVITY_1 -> {
                // edit text
                data!!
                this.text = data.getStringExtra(DiaryFileLibraryEditTextActivity.EXTRA_RESULT)
                topDynamicLL.length_tv?.text = text?.length.toString()
            }
            else -> {
            }
        }
    }

    override fun finish() {
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_RESULT_IDENTIFIER, resultIdentifier)
        setResult(0, resultIntent)
        super.finish()
    }

    companion object {
        /**
         * result string intent
         */
        const val EXTRA_RESULT_IDENTIFIER = "resultIdentifier"
    }
}