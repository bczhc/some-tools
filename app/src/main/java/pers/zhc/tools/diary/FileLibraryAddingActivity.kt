package pers.zhc.tools.diary

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import kotlinx.android.synthetic.main.diary_attachment_file_library_adding_file_activity.*
import kotlinx.android.synthetic.main.diary_file_library_copy_progress_view.view.*
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
    private lateinit var storageTypeRG: RadioGroup
    private lateinit var descriptionET: EditText
    private lateinit var pickedFileET: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_file_library_adding_file_activity)

        val pickFileBtn = pick_file_btn!!
        descriptionET = description_et!!
        val submitBtn = submit_btn!!
        pickedFileET = picked_file_et.editText
        storageTypeRG = storage_type_rg!!

        pickFileBtn.setOnClickListener {
            val intent = Intent(this, FilePicker::class.java)
            intent.putExtra("option", FilePicker.PICK_FILE)
            startActivityForResult(intent, RequestCode.START_ACTIVITY_0)
        }

        submitBtn.setOnClickListener {
            val progressView = View.inflate(this, R.layout.diary_file_library_copy_progress_view, null)
            val msgTV = progressView.msg_tv

            val dialog = Dialog(this)
            DialogUtil.setDialogAttr(dialog,
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

            val identifier = DiaryMainActivity.computeFileIdentifier(File(pickedFileET.text.toString()))

            msgTV.setText(R.string.insert_record)
            val filename = File(pickedFileET.text.toString()).name
            val storageTypeOrdinal = getStorageTypeEnum().enumInt
            val description = descriptionET.text.toString()

            val hasRecord =
                diaryDatabase.hasRecord("SELECT * FROM diary_attachment_file WHERE identifier IS '$identifier'")
            if (hasRecord) {
                DialogUtil.createConfirmationAlertDialog(this, { _, _ ->
                    val statement =
                        diaryDatabase.compileStatement("UPDATE diary_attachment_file\nSET filename     = ?,\n    storage_type = $storageTypeOrdinal,\n    description  = ?\nWHERE identifier IS ?")
                    statement.reset()
                    statement.bindText(1, filename)
                    statement.bindText(2, description)
                    statement.bindText(3, identifier)
                    statement.step()
                    statement.release()

                    ToastUtils.show(this, R.string.updating_done)
                }, R.string.file_exists_alert_msg).show()
            } else {
                val statement =
                    diaryDatabase.compileStatement("INSERT INTO diary_attachment_file (identifier, add_timestamp, filename, storage_type, description)\nVALUES (?, ?, ?, ?, ?)")
                statement.reset()
                statement.bindText(1, identifier)
                statement.bind(2, System.currentTimeMillis())
                statement.bindText(3, filename)
                statement.bind(4, storageTypeOrdinal)
                statement.bindText(5, description)
                statement.step()
                statement.release()

                msgTV.setText(R.string.copying_file)
                FileUtil.copy(pickedFileET.text.toString(),
                    DiaryAttachmentSettingsActivity.getFileStoragePath(diaryDatabase)!!)

                msgTV.setText(R.string.done)
                dialog.dismiss()
                ToastUtils.show(this, R.string.adding_done)
                finish()
            }
        }
    }

    private fun getStorageTypeEnum(): FileLibraryActivity.StorageType {
        return when (storageTypeRG.checkedRadioButtonId) {
            R.id.text_radio -> {
                FileLibraryActivity.StorageType.TEXT
            }
            else -> {
                FileLibraryActivity.StorageType.RAW
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data!!
        val pickedFile = data.getStringExtra("result") ?: return
        pickedFileET.setText(pickedFile)
    }
}