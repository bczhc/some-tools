package pers.zhc.tools.diary

import android.os.Bundle
import kotlinx.android.synthetic.main.diary_attachment_settings_activity.*
import org.json.JSONObject
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.sqlite.SQLite3

/**
 * @author bczhc
 */
class DiaryAttachmentSettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val diaryDatabase = DiaryMainActivity.getDiaryDatabase(this)
        setContentView(R.layout.diary_attachment_settings_activity)

        val storagePathTV = storage_path_tv!!

        val fileStoragePath = getFileStoragePath(diaryDatabase)
        if
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
                jsonObject.getString("diaryAttachmentFileLibraryStoragePath")
            }
        }
    }

    fun getDefaultStoragePath(): String {
//        Common.
    }
}