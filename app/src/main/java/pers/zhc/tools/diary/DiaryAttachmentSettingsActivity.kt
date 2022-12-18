package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.diary_attachment_settings_activity.*
import org.json.JSONObject
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.utils.nullMap
import java.io.File

/**
 * @author bczhc
 */
class DiaryAttachmentSettingsActivity : DiaryBaseActivity() {
    private lateinit var storagePathTV: TextView
    private lateinit var oldPathStr: String

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
            val intent = Intent(this, FilePicker::class.java)
            intent.putExtra("option", FilePicker.PICK_FOLDER)
            startActivityForResult(intent, RequestCode.START_ACTIVITY_0)
        }

        restoreToDefaultBtn.setOnClickListener {
            storagePathTV.text = getDefaultStoragePath()
        }

        doneBtn.setOnClickListener { save() }
    }

    private fun save() {
        DialogUtil.createAlertDialogWithNeutralButton(
            this,
            { _, _ ->
                ToastUtils.show(this, R.string.save_success_toast)
            },
            { _, _ ->
                diaryDatabase.updateExtraInfo(ExtraInfo(storagePathTV.text.toString()))
                ToastUtils.show(this, R.string.save_success_toast)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data!!
        val result = data.getStringExtra("result") ?: return
        storagePathTV.text = result
    }

    override fun onBackPressed() {
        // TODO
        if (storagePathTV.text.toString() != oldPathStr) {
            // has changed the storage path
            DialogUtil.createConfirmationAlertDialog(this, { _, _ ->
                save()
                super.onBackPressed()
            }, R.string.diary_setting_storage_path_changed_dialog_msg).show()
        } else super.onBackPressed()
    }
}