package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import kotlinx.android.synthetic.main.diary_attachment_settings_activity.*
import org.json.JSONObject
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.sqlite.SQLite3
import java.io.File

/**
 * @author bczhc
 */
class DiaryAttachmentSettingsActivity : DiaryBaseActivity() {
    private lateinit var storagePathTV: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_settings_activity)

        storagePathTV = storage_path_tv!!
        val changeBtn = change_btn!!
        val doneBtn = done_btn
        val restoreToDefaultBtn = restore_to_default_btn

        var fileStoragePath = getFileStoragePath(diaryDatabase)
        if (fileStoragePath == null) {
            val defaultInfoJson = JSONObject()
            defaultInfoJson.put(storagePathJsonKey, getDefaultStoragePath())

            val statement =
                diaryDatabase.compileStatement("INSERT INTO diary_attachment_info (info_json)\nVALUES (?)")
            statement.bindText(1, defaultInfoJson.toString())
            statement.step()
            statement.release()
        }

        fileStoragePath = getFileStoragePath(diaryDatabase)
        storagePathTV.text = getString(R.string.str, fileStoragePath)

        changeBtn.setOnClickListener {
            ToastUtils.show(this, R.string.pick_folder)
            val intent = Intent(this, FilePicker::class.java)
            intent.putExtra("option", FilePicker.PICK_FOLDER)
            startActivityForResult(intent, RequestCode.START_ACTIVITY_0)
        }

        restoreToDefaultBtn.setOnClickListener {
            changeStoragePath(getDefaultStoragePath())
        }

        doneBtn.setOnClickListener { finish() }
    }

    companion object {
        fun getFileStoragePath(diaryDatabase: SQLite3): String? {
            var infoJSON: String? = null

            diaryDatabase.exec("SELECT info_json\nFROM diary_attachment_info") { content ->
                infoJSON = content[0]
                return@exec 0
            }

            return if (infoJSON == null) null
            else {
                val jsonObject = JSONObject(infoJSON!!)
                jsonObject.getString(storagePathJsonKey)
            }
        }

        const val storagePathJsonKey = "diaryAttachmentFileLibraryStoragePath"
    }

    fun getDefaultStoragePath(): String {
        val file = File(Common.getAppMainExternalStoragePathFile(this), "diary-attachment-files")
        file.mkdirs()
        return file.path
    }

    private fun changeStoragePath(newStoragePath: String) {
        var statement = diaryDatabase.compileStatement("SELECT info_json\nFROM diary_attachment_info")
        statement.stepRow()
        val infoJSON = statement.cursor.getText(statement.getIndexByColumnName("info_json"))
        statement.release()

        val infoJONObject = JSONObject(infoJSON)
        infoJONObject.put(storagePathJsonKey, newStoragePath)

        statement = diaryDatabase.compileStatement("UPDATE diary_attachment_info\nSET info_json = ?")
        statement.bindText(1, infoJONObject.toString())
        statement.step()
        statement.release()

        val fileStoragePath = getFileStoragePath(diaryDatabase)
        storagePathTV.text = getString(R.string.str, fileStoragePath)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data!!
        val result = data.getStringExtra("result") ?: return
        changeStoragePath(result)
    }
}