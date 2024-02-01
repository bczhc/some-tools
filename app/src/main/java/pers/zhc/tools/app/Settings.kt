package pers.zhc.tools.app

import androidx.appcompat.app.AppCompatDelegate
import pers.zhc.tools.MyApplication
import pers.zhc.tools.MyApplication.Companion.GSON
import pers.zhc.tools.utils.fromJsonOrNull
import pers.zhc.tools.utils.illegalArgument
import java.io.File

data class Settings(
    var serverUrl: AppServerUrl? = null,
    var theme: AppTheme? = null,
) {
    companion object {
        private const val JSON_FILENAME = "settings.json"
        val JSON_FILE by lazy {
            File(MyApplication.appContext.filesDir , JSON_FILENAME)
        }

        data class AppServerUrl(
            var serverRootUrl: String,
            var staticSourceRootUrl: String,
            var githubRawRootUrl: String,
        )

        enum class AppTheme {
            LIGHT, DARK, FOLLOW_SYSTEM;

            fun toNightModeOption(): Int = when (this) {
                LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                DARK -> AppCompatDelegate.MODE_NIGHT_YES
                FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            companion object {
                fun fromNightModeOption(option: Int): AppTheme {
                    return when (option) {
                        AppCompatDelegate.MODE_NIGHT_NO -> LIGHT
                        AppCompatDelegate.MODE_NIGHT_YES -> DARK
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> FOLLOW_SYSTEM
                        else -> illegalArgument()
                    }
                }
            }
        }

        fun default() = Settings()

        fun readSettings(): Settings {
            if (!JSON_FILE.exists()) {
                writeSettings(default())
            }

            return GSON.fromJsonOrNull(JSON_FILE.readText(), Settings::class.java)
                ?: default()
        }

        fun writeSettings(settings: Settings) {
            val json = GSON.toJson(settings)
            JSON_FILE.writeText(json)
        }

        fun updateSettings(block: (settings: Settings) -> Unit) {
            val settings = readSettings()
            block(settings)
            writeSettings(settings)
        }
    }
}
