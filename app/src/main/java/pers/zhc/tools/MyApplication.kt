package pers.zhc.tools

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager.WakeLock
import androidx.appcompat.app.AppCompatDelegate
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.plugins.*
import org.json.JSONObject
import pers.zhc.tools.MyApplication.Companion.InfoJson.Companion.KEY_GITHUB_RAW_ROOT_URL
import pers.zhc.tools.MyApplication.Companion.InfoJson.Companion.KEY_SERVER_ROOT_URL
import pers.zhc.tools.MyApplication.Companion.InfoJson.Companion.KEY_STATIC_RESOURCE_ROOT_URL
import pers.zhc.tools.crashhandler.CrashHandler
import pers.zhc.tools.email.ContactActivity
import pers.zhc.tools.email.Database
import pers.zhc.tools.words.WordsMainActivity
import pers.zhc.tools.wubi.DictionaryDatabase
import pers.zhc.tools.wubi.SingleCharCodesChecker
import pers.zhc.tools.wubi.WubiInverseDictManager
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
        appContext = this
        CrashHandler.install(this)
        WordsMainActivity.init(this)
        Database.initPath(this)
        ContactActivity.initPath(this)
        DictionaryDatabase.init(this)
        WubiInverseDictManager.init(this)
        SingleCharCodesChecker.RecordDatabase.init(this)

        registerNotificationChannel()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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
        infoFile = File(filesDir, InfoJson.FILENAME)
        if (!infoFile.exists()) {
            infoFile.createNewFile()
            infoFile.writeText(JSONObject().toString(4))
        }
        val read = infoFile.readText()
        val jsonObject = JSONObject(read)

        if (jsonObject.has(KEY_SERVER_ROOT_URL)) {
            Info.serverRootURL = jsonObject.getString(KEY_SERVER_ROOT_URL)
        }
        if (jsonObject.has(KEY_STATIC_RESOURCE_ROOT_URL)) {
            Info.staticResourceRootURL = jsonObject.getString(KEY_STATIC_RESOURCE_ROOT_URL)
        }
        if (jsonObject.has(KEY_GITHUB_RAW_ROOT_URL)) {
            Info.githubRawRootURL = jsonObject.getString(KEY_GITHUB_RAW_ROOT_URL)
        }
    }

    companion object {
        private lateinit var infoFile: File
        lateinit var appContext: Context

        // the default global Gson
        val GSON = Gson()

        val HTTP_CLIENT_DEFAULT = HttpClient()
        val HTTP_CLIENT_DOWNLOAD = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            }
        }

        @JvmField
        var wakeLock: WakeLock? = null

        class InfoJson {
            companion object {
                const val FILENAME = "info.json"
                const val KEY_SERVER_ROOT_URL = "serverRootURL"
                const val KEY_STATIC_RESOURCE_ROOT_URL = "staticResourceRootURL"
                const val KEY_GITHUB_RAW_ROOT_URL = "githubRawRootURL"
            }
        }

        fun getInfoJSON() {
            TODO("Not yet implemented")
        }

        fun writeInfoJSON(info: JSONObject) {
            infoFile.writeText(info.toString(4))
        }

        var NOTIFICATION_CHANNEL_ID_UNIVERSAL = "c1"

        init {
            System.loadLibrary("Main")
            System.loadLibrary("jni-lib")
            System.loadLibrary("rust_jni")
        }
    }
}
