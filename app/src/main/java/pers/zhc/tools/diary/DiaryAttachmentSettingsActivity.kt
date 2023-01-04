package pers.zhc.tools.diary

import android.os.Bundle
import android.widget.TextView
import kotlinx.android.synthetic.main.diary_attachment_settings_activity.*
import org.apache.commons.io.FileUtils
import pers.zhc.tools.R
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

        val storagePath = LocalInfo.attachmentStoragePath

        storagePathTV.text = getString(R.string.str, storagePath)
        oldPathStr = storagePathTV.text.toString()

        changeBtn.setOnClickListener {
            ToastUtils.show(this, R.string.pick_folder)
            launchers.pickFolder.launch(Unit)
        }

        restoreToDefaultBtn.setOnClickListener {
            storagePathTV.text = LocalInfo.getDefaultStoragePath()
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

    private fun save(onFinished: () -> Unit = {}) {
        DialogUtil.createAlertDialogWithNeutralButton(
            this,
            { _, _ ->
                // move file
                val progressDialog = ProgressDialog(this).also {
                    it.getProgressView().apply {
                        setIsIndeterminateMode(true)
                        setTitle(getString(R.string.moving_files_progress_dialog))
                    }
                    it.show()
                }
                Thread {
                    val newPath = storagePathTV.text.toString()
                    val result = moveOldFiles(File(newPath))
                    runOnUiThread { progressDialog.dismiss() }
                    if (!result) {
                        ToastUtils.show(this, R.string.moving_file_failed)
                    } else {
                        ToastUtils.show(this, R.string.save_success_toast)
                    }
                    LocalInfo.attachmentStoragePath = newPath
                    oldPathStr = newPath
                    onFinished()
                }.start()
            },
            { _, _ ->
                // don't move file
                val newPath = storagePathTV.text.toString()
                LocalInfo.attachmentStoragePath = newPath
                ToastUtils.show(this, R.string.save_success_toast)
                oldPathStr = newPath
                onFinished()
            }, R.string.diary_attachment_setting_move_file_dialog_title
        ).apply {
            setMessage(getString(R.string.diary_attachment_setting_move_file_dialog_message))
        }.show()
    }

    override fun onBackPressed() {
        if (storagePathTV.text.toString() != oldPathStr) {
            // has changed the storage path
            DialogUtil.createConfirmationAlertDialog(this, { _, _ ->
                save {
                    runOnUiThread {
                        super.onBackPressed()
                    }
                }
            }, R.string.diary_setting_storage_path_changed_dialog_msg).show()
        } else super.onBackPressed()
    }
}