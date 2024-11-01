package pers.zhc.tools.diary

import pers.zhc.tools.MyApplication
import pers.zhc.tools.MyApplication.Companion.GSON
import pers.zhc.tools.utils.fromJsonOrNull
import java.io.File

data class LocalConfig(
    var password: String? = "",
    var uiPassword: String? = "",
) {
    companion object {
        private val FILE_PATH by lazy {
            File(MyApplication.appContext.filesDir, "diary-config.json").also {
                if (!it.exists()) {
                    it.writeText("{}")
                }
            }
        }

        fun read(): LocalConfig {
            return GSON.fromJsonOrNull(FILE_PATH.readText(), LocalConfig::class.java) ?: LocalConfig()
        }

        fun write(config: LocalConfig) {
            FILE_PATH.writeText(GSON.toJson(config))
        }

        fun updatePassword(password: String) {
            write(read().apply {
                this.password = password
            })
        }

        fun readPassword(): String {
            return (read().password ?: "").ifEmpty { DiaryDatabase.DEFAULT_PASSPHRASE }
        }
    }
}
