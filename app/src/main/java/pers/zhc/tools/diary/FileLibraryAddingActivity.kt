package pers.zhc.tools.diary

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.diary_attachment_file_library_adding_file_activity.*
import kotlinx.android.synthetic.main.diary_file_library_copy_progress_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.utils.FileUtil
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.sqlite.SQLite3
import java.io.File

/**
 * @author bczhc
 */
class FileLibraryAddingActivity : DiaryBaseActivity() {
    private var storageTypeSpinnerSelectedPos: Int = 0
    private lateinit var storageTypeValues: Array<FileLibraryActivity.StorageType>
    private lateinit var descriptionET: EditText
    private lateinit var pickedFileET: EditText
    private lateinit var spinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_file_library_adding_file_activity)

        val pickFileBtn = pick_file_btn!!
        descriptionET = description_et!!
        val submitBtn = submit_btn!!
        pickedFileET = picked_file_et.editText
        spinner = findViewById(R.id.spinner)!!

        pickFileBtn.setOnClickListener {
            val intent = Intent(this, FilePicker::class.java)
            intent.putExtra("option", FilePicker.PICK_FILE)
            startActivityForResult(intent, RequestCode.START_ACTIVITY_0)
        }

        storageTypeValues = FileLibraryActivity.StorageType.values()
        storageTypeValues[0].toString()
        val arrayAdapter =
            object : ArrayAdapter<FileLibraryActivity.StorageType>(
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
                storageTypeSpinnerSelectedPos = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        spinner.setSelection(0)

        submitBtn.setOnClickListener {
            val progressView = View.inflate(this, R.layout.diary_file_library_copy_progress_view, null)
            val msgTV = progressView.msg_tv

            if (!File(pickedFileET.text.toString()).exists()) {
                ToastUtils.show(this, R.string.file_not_exist_alert_msg)
                return@setOnClickListener
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
                val identifier = DiaryMainActivity.computeFileIdentifier(File(pickedFileET.text.toString()))
                runOnUiThread { msgTV.setText(R.string.insert_record) }

                val filename = File(pickedFileET.text.toString()).name
                val storageTypeOrdinal = getStorageTypeEnum().enumInt
                val description = descriptionET.text.toString()

                val hasRecord =
                    diaryDatabase.hasRecord("SELECT * FROM diary_attachment_file WHERE identifier IS '$identifier'")
                if (hasRecord) {
                    runOnUiThread {
                        DialogUtil.createConfirmationAlertDialog(this, { _, _ ->
                            updateFileRecord(storageTypeOrdinal, filename, description, identifier)
                            runOnUiThread { dialog.dismiss() }
                            ToastUtils.show(this, R.string.updating_done)
                        }, { _, _ ->
                            runOnUiThread { dialog.dismiss() }
                        }, R.string.file_exists_alert_msg).show()
                    }
                } else {
                    insertFileRecord(identifier, filename, storageTypeOrdinal, description)

                    runOnUiThread { msgTV.setText(R.string.copying_file) }
                    FileUtil.copy(
                        pickedFileET.text.toString(),
                        File(DiaryAttachmentSettingsActivity.getFileStoragePath(diaryDatabase)!!, identifier).path
                    )

                    runOnUiThread {
                        msgTV.setText(R.string.done)
                        dialog.dismiss()
                        ToastUtils.show(this, R.string.adding_done)
                        finish()
                    }
                }
            }.start()
        }
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

    private fun insertFileRecord(
        identifier: String,
        filename: String,
        storageTypeOrdinal: Int,
        description: String,
    ) {
        val statement =
            diaryDatabase.compileStatement("INSERT INTO diary_attachment_file (identifier, addition_timestamp, filename, storage_type, description)\nVALUES (?, ?, ?, ?, ?)")
        statement.reset()
        statement.bindText(1, identifier)
        statement.bind(2, System.currentTimeMillis())
        statement.bindText(3, filename)
        statement.bind(4, storageTypeOrdinal)
        statement.bindText(5, description)
        statement.step()
        statement.release()
    }

    private fun getStorageTypeEnum(): FileLibraryActivity.StorageType {
        return this.storageTypeValues[this.storageTypeSpinnerSelectedPos]
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data!!
        val pickedFile = data.getStringExtra("result") ?: return
        pickedFileET.setText(pickedFile)
    }
}