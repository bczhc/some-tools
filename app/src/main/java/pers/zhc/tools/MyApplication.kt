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
import pers.zhc.tools.app.Settings
import pers.zhc.tools.app.Settings.Companion.AppTheme
import pers.zhc.tools.crashhandler.CrashHandler
import pers.zhc.tools.email.ContactActivity
import pers.zhc.tools.email.Database
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.Common
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
        staticInit(this)
        CrashHandler.install(this)
        WordsMainActivity.init(this)
        Database.initPath(this)
        ContactActivity.initPath(this)
        DictionaryDatabase.init(this)
        WubiInverseDictManager.init(this)
        SingleCharCodesChecker.RecordDatabase.init(this)

        registerNotificationChannel()
        initJniFields()

        val appTheme = Settings.readSettings().theme ?: AppTheme.FOLLOW_SYSTEM
        AppCompatDelegate.setDefaultNightMode(appTheme.toNightModeOption())
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

    private fun initJniFields() {
        JNI.rustSetUpStaticFields(arrayOf(crashLogDir.path))
    }

    companion object {
        lateinit var appContext: Context
        lateinit var crashLogDir: File

        // the default global Gson
        val GSON by lazy { Gson() }

        val HTTP_CLIENT_DEFAULT = HttpClient()
        val HTTP_CLIENT_DOWNLOAD = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            }
        }

        @JvmField
        var wakeLock: WakeLock? = null

        var NOTIFICATION_CHANNEL_ID_UNIVERSAL = "c1"

        private fun staticInit(context: Context) {
            appContext = context
            crashLogDir = File(Common.getAppMainExternalStoragePath(context) + File.separatorChar + "crash")
            JNI.initialize()
        }
    }
}
