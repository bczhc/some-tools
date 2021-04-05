package pers.zhc.tools.diary

import android.os.Bundle
import android.view.MenuItem
import org.intellij.lang.annotations.Language
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.sqlite.SQLite3

/**
 * @author bczhc
 */
open class DiaryBaseActivity() : BaseActivity() {
    companion object {
        var diaryDatabaseRef: DiaryDatabaseRef? = null

        @JvmStatic
        protected fun initDatabase(database: SQLite3) {
            @Language("SQLite") val statements =
                "-- main diary content table\nCREATE TABLE IF NOT EXISTS diary\n(\n    date    INTEGER PRIMARY KEY,\n    content TEXT NOT NULL\n);\n-- diary attachment file info table\n-- identifier: SHA1(hex(file).concat(packIntLittleEndian(file.length)))\nCREATE TABLE IF NOT EXISTS diary_attachment_file\n(\n    identifier    TEXT NOT NULL PRIMARY KEY,\n    add_timestamp INTEGER UNIQUE,\n    filename      TEXT NOT NULL,\n    storage_type  INTEGER,\n    description   TEXT NOT NULL\n);\n-- diary attachment file reference table; an attachment can have multiple file references\nCREATE TABLE IF NOT EXISTS diary_attachment_file_reference\n(\n    attachment_id   INTEGER,\n    file_identifier TEXT NOT NULL\n);\n-- diary attachment data table\nCREATE TABLE IF NOT EXISTS diary_attachment\n(\n    id          INTEGER PRIMARY KEY,\n    title       TEXT NOT NULL,\n    description TEXT NOT NULL\n);\n-- diary attachment settings info table\nCREATE TABLE IF NOT EXISTS diary_attachment_info\n(\n    info_json TEXT NOT NULL PRIMARY KEY\n);\n-- a mapping table between diary and attachment; a diary can have multiple attachments\nCREATE TABLE IF NOT EXISTS diary_attachment_mapping\n(\n    diary_date             INTEGER,\n    referred_attachment_id INTEGER UNIQUE\n)".split(
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
        if (diaryDatabaseRef == null || diaryDatabaseRef!!.isAbandoned()) {
            internalDatabasePath = Common.getInternalDatabaseDir(this, "diary.db").path
            setDatabase(internalDatabasePath)
        }
        diaryDatabaseRef!!.countRef()
        this.diaryDatabase = diaryDatabaseRef!!.database

        val actionBar = this.supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)
    }

    override fun finish() {
        diaryDatabaseRef!!.countDownRef()
        super.finish()
    }

    /**
     * Open or change database.
     */
    protected fun setDatabase(path: String) {
        val database = SQLite3.open(path)
        if (diaryDatabaseRef != null && !diaryDatabaseRef!!.isAbandoned()) {
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

        /**
         * Return if the database this reference object maintained is released or closed.
         * If returns true, this reference object should be instantiated again.
         */
        fun isAbandoned(): Boolean {
            return this.diaryDatabaseRefCount == 0
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) onBackPressed()
        return super.onOptionsItemSelected(item)
    }
}