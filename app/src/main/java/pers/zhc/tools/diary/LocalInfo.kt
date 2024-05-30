package pers.zhc.tools.diary

import pers.zhc.tools.MyApplication
import pers.zhc.tools.utils.Common
import java.io.File

/**
 * merge into [LocalConfig]
  */
data class LocalInfo(
    val attachmentStoragePath: String?,
) {
    companion object {
        var attachmentStoragePath: String
            get() {
                val info = LocalInfoDatabase.getInfo()
                if (info?.attachmentStoragePath == null) {
                    val defaultStoragePath = getDefaultStoragePath()
                    LocalInfoDatabase.updateInfo(LocalInfo(defaultStoragePath))
                    return defaultStoragePath
                }
                return info.attachmentStoragePath
            }
            set(value) {
                LocalInfoDatabase.updateInfo(LocalInfo(value))
            }

        fun getDefaultStoragePath(): String {
            val file =
                File(Common.getAppMainExternalStoragePathFile(MyApplication.appContext), "diary-attachment-files")
            file.mkdirs()
            return file.path
        }
    }
}
