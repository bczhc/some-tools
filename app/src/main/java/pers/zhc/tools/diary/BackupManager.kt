package pers.zhc.tools.diary

import pers.zhc.tools.MyApplication
import pers.zhc.tools.utils.*
import java.io.File

object BackupManager {
    private val bakDirs by lazy {
        val dir1 = File(MyApplication.appContext.filesDir, "diary-bak").also {
            it.requireMkdirs()
        }
        val dir2 = File(MyApplication.appContext.externalFilesDir(), "diary-bak").also {
            it.requireMkdirs()
        }
        arrayOf(dir1, dir2)
    }

    fun checkBakDirs() {
        bakDirs.forEach { d ->
            File(d, timestampMillis().toString()).apply {
                val os = outputStream()
                os.write(os.toString().toByteArray())
                os.flush()
                requireDelete()
            }
        }
    }

    fun backup(file: File) {
        bakDirs.forEach { d ->
            File(d, "${timestamp()}-${UUID.randomNoDash().substring(0 until 5)}").apply {
                copyFrom(file.inputStream())
            }
        }
    }
}
