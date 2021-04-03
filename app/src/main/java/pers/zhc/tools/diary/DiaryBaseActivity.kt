package pers.zhc.tools.diary

import android.os.Bundle
import org.intellij.lang.annotations.Language
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.sqlite.SQLite3

/**
 * @author bczhc
 */
open class DiaryBaseActivity : BaseActivity() {
    companion object {
        var diaryDatabaseRef: DiaryDatabaseRef? = null

        @JvmStatic
        protected fun initDatabase(database: SQLite3) {
            @Language("SQLite") val statements =
                "-- main diary content table\nCREATE TABLE IF NOT EXISTS diary\n(\n    date          INTEGER PRIMARY KEY,\n    content       TEXT NOT NULL,\n    attachment_id INTEGER\n);\n-- diary attachment file info table\n-- identifier: sha1 + file length\nCREATE TABLE IF NOT EXISTS diary_attachment_file\n(\n    identifier    TEXT NOT NULL PRIMARY KEY,\n    add_timestamp INTEGER,\n    filename      TEXT NOT NULL,\n    storage_type  INTEGER,\n    description   TEXT NOT NULL\n);\n-- diary attachment file reference table, an attachment can have multiple file reference\nCREATE TABLE IF NOT EXISTS diary_attachment_file_reference\n(\n    attachment_id   INTEGER,\n    file_identifier TEXT NOT NULL\n);\n-- diary attachment data table\nCREATE TABLE IF NOT EXISTS diary_attachment\n(\n    id          INTEGER PRIMARY KEY,\n    title       TEXT NOT NULL,\n    description TEXT NOT NULL\n);\n-- diary attachment settings info table\nCREATE TABLE IF NOT EXISTS diary_attachment_info\n(\n    info_json TEXT NOT NULL PRIMARY KEY\n);".split(
                    ";\n")
            statements.forEach {
                database.exec(it)
            }
        }
    }

    protected lateinit var internalDatabasePath: String
    protected lateinit var diaryDatabase: SQLite3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (diaryDatabaseRef == null) {
            internalDatabasePath = Common.getInternalDatabaseDir(this, "diary.db").path
            setDatabase(internalDatabasePath)
        }
        diaryDatabaseRef!!.countRef()
        this.diaryDatabase = diaryDatabaseRef!!.database
    }

    override fun finish() {
        diaryDatabaseRef!!.countDownRef()
        super.finish()
    }

    /**
     * Open or change database.
     */
    private fun setDatabase(path: String) {
        val database = SQLite3.open(path)
        if (diaryDatabaseRef != null) {
            diaryDatabaseRef!!.close()
        }
        diaryDatabaseRef = DiaryDatabaseRef(database)
        initDatabase(database)

    }

    class DiaryDatabaseRef internal constructor(val database: SQLite3) {
        private var diaryDatabaseRefCount = 0

        fun countRef() {
            ++diaryDatabaseRefCount
        }

        fun countDownRef() {
            if (--diaryDatabaseRefCount == 0) {
                close()
            }
        }

        internal fun close() {
            database.close()
        }
    }
}