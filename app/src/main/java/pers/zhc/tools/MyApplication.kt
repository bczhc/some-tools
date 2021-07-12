package pers.zhc.tools

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONException
import org.json.JSONObject
import pers.zhc.tools.crashhandler.CrashHandler
import pers.zhc.tools.diary.DiaryDatabase
import java.io.File

/**
 * @author bczhc
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        CrashHandler.install(this)
        DiaryDatabase.init(this)
        registerNotificationChannel()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
        initAppInfoFile()
    }

    private fun registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_UNIVERSAL,
                getString(R.string.notification_channel_name_common),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun initAppInfoFile() {
        infoFile = File(filesDir, "info.json")
        if (!infoFile.exists()) {
            infoFile.createNewFile()
            infoFile.writeText(JSONObject().toString(4))
        }
        val read = infoFile.readText()
        val jsonObject = JSONObject(read)
        try {
            Infos.serverURL = jsonObject.getString("serverURL")
        } catch (_: JSONException) {
        }
        try {
            Infos.resourceURL = jsonObject.getString("resourceURL")
        } catch (_: JSONException) {
        }
    }

    companion object {
        private lateinit var infoFile: File

        fun getInfoJSON() {
            TODO("Not yet implemented")
        }

        fun writeInfoJSON(info: JSONObject) {
            infoFile.writeText(info.toString(4))
        }

        var NOTIFICATION_CHANNEL_ID_UNIVERSAL = "c1"
    }
}