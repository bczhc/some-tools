package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.diary_attachment_settings_activity.*
import org.apache.commons.io.FileUtils
import org.json.JSONObject
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.filepicker.FilePickerActivityContract
import pers.zhc.tools.utils.*
import java.io.File
import java.io.IOException

/**
 * @author bczhc
 */
class DiaryAttachmentSettingsActivity : DiaryBaseActivity() {
    private lateinit var storagePathTV: TextView
    private lateinit var oldPathStr: String

    private val launchers = object {
        val pickFolder = registerForActivityResult(
            FilePickerActivityContract(
                FilePickerActivityContract.FilePickerType.PICK_FOLDER,
                false
            )
        ) { result ->
            result ?: return@registerForActivityResult
            storagePathTV.text = result.path
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_settings_activity)

        storagePathTV = storage_path_tv!!
        val changeBtn = change_btn!!
        val doneBtn = done_btn
        val restoreToDefaultBtn = restore_to_default_btn

        val storagePath = diaryDatabase.queryExtraInfo().nullMap { it.diaryAttachmentFileLibraryStoragePath }
            ?: getDefaultStoragePath().also {
                diaryDatabase.updateExtraInfo(ExtraInfo(it))
            }

        storagePathTV.text = getString(R.string.str, storagePath)
        oldPathStr = storagePathTV.text.toString()

        changeBtn.setOnClickListener {
            ToastUtils.show(this, R.string.pick_folder)
            launchers.pickFolder.launch(Unit)
        }

        restoreToDefaultBtn.setOnClickListener {
            storagePathTV.text = getDefaultStoragePath()
        }

        doneBtn.setOnClickListener { save() }
    }

    /**
     * returns if it succeeds
     */
    private fun moveOldFiles(newPath: File): Boolean {
        val children = File(oldPathStr).listFiles() ?: return false
        try {
            children.filter { it.isFile }.forEach {
                FileUtils.moveFile(it, File(newPath, it.name))
            }
        } catch (_: IOException) {
            return false
        }

        return true
    }

    private fun save() {
        DialogUtil.createAlertDialogWithNeutralButton(
            this,
            { _, _ ->
                // don't move file
                diaryDatabase.updateExtraInfo(ExtraInfo(storagePathTV.text.toString()))
                ToastUtils.show(this, R.string.save_success_toast)
            },
            { _, _ ->
                // move file
                val progressDialog = ProgressDialog(this).also {
                    it.getProgressView().apply {
                        setIsIndeterminateMode(true)
                        setText(getString(R.string.moving_files_progress_dialog))
                    }
                    it.show()
                }
                Thread {
                    val result = moveOldFiles(File(storagePathTV.text.toString()))
                    progressDialog.dismiss()
                    if (!result) {
                        ToastUtils.show(this, R.string.moving_file_failed)
                    } else {
                        ToastUtils.show(this, R.string.save_success_toast)
                    }
                    diaryDatabase.updateExtraInfo(ExtraInfo(storagePathTV.text.toString()))
                }

            }, R.string.diary_attachment_setting_move_file_dialog_title
        ).apply {
            setMessage(getString(R.string.diary_attachment_setting_move_file_dialog_message))
        }.show()
    }

    private fun getDefaultStoragePath(): String {
        val file = File(Common.getAppMainExternalStoragePathFile(this), "diary-attachment-files")
        file.mkdirs()
        return file.path
    }

    override fun onBackPressed() {
        if (storagePathTV.text.toString() != oldPathStr) {
            // has changed the storage path
            DialogUtil.createConfirmationAlertDialog(this, { _, _ ->
                save()
                super.onBackPressed()
            }, R.string.diary_setting_storage_path_changed_dialog_msg).show()
        } else super.onBackPressed()
    }
}