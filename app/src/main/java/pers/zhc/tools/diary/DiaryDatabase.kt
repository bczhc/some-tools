package pers.zhc.tools.diary

import android.content.Context
import org.intellij.lang.annotations.Language
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.RefCountHolder
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.util.Assertion

/**
 * @author bczhc
 */
class DiaryDatabase {
    companion object {
        private var holder: RefCountHolder<SQLite3>? = null
        lateinit var internalDatabasePath: String

        @JvmStatic
        fun init(ctx: Context) {
            internalDatabasePath = Common.getInternalDatabaseDir(ctx, "diary.db").path
        }

        fun getDatabaseRef(): SQLite3 {
            if (holder == null || holder!!.isAbandoned) {
                val db = SQLite3.open(internalDatabasePath)
                initDatabase(db)
                holder = object : RefCountHolder<SQLite3>(db) {
                    override fun onClose(obj: SQLite3?) {
                        obj!!.close()
                    }
                }
            }
            return holder!!.newRef()
        }

        fun releaseDatabaseRef() {
            if (holder != null) {
                holder!!.releaseRef()
            }
        }

        fun getHolder(): RefCountHolder<SQLite3> {
            return holder!!
        }

        fun changeDatabase(path: String) {
            val refCount = holder!!.refCount
            if (refCount != 0) {
                throw CloseFailureException(refCount)
            }

            val new = SQLite3.open(path)
            holder = object : RefCountHolder<SQLite3>(new) {
                override fun onClose(obj: SQLite3?) {
                    obj!!.close()
                }
            }
        }

        class CloseFailureException(val refCount: Int) : Exception()

        protected fun initDatabase(database: SQLite3) {
            @Language("SQLite") val statements =
                """PRAGMA foreign_keys = ON;
-- main diary content table
CREATE TABLE IF NOT EXISTS diary
(
    "date"  INTEGER PRIMARY KEY,
    content TEXT NOT NULL
);
-- diary attachment file info table
-- identifier: SHA1(hex(file).concat(packIntLittleEndian(file.length)))
CREATE TABLE IF NOT EXISTS diary_attachment_file
(
    identifier         TEXT NOT NULL PRIMARY KEY,
    -- Enum:
    -- RAW 0
    -- TEXT 1
    -- IMAGE 2
    -- AUDIO 3
    storage_type       INTEGER,
    addition_timestamp INTEGER UNIQUE,
    filename           TEXT,
    description        TEXT NOT NULL
);
-- diary attachment text storage table
CREATE TABLE IF NOT EXISTS diary_attachment_text
(
    identifier TEXT NOT NULL PRIMARY KEY,
    content    TEXT NOT NULL,

    FOREIGN KEY (identifier) REFERENCES diary_attachment_file (identifier)
);
-- diary attachment file reference table; an attachment can have multiple file references
CREATE TABLE IF NOT EXISTS diary_attachment_file_reference
(
    attachment_id INTEGER,
    identifier    TEXT NOT NULL,

    FOREIGN KEY (attachment_id) REFERENCES diary_attachment (id),
    FOREIGN KEY (identifier) REFERENCES diary_attachment_file (identifier)
);
-- diary attachment data table
CREATE TABLE IF NOT EXISTS diary_attachment
(
    id          INTEGER PRIMARY KEY,
    title       TEXT NOT NULL,
    description TEXT NOT NULL
);
-- diary attachment settings info table
CREATE TABLE IF NOT EXISTS diary_attachment_info
(
    info_json TEXT NOT NULL PRIMARY KEY
);
-- a mapping table between diary and attachment; a diary can have multiple attachments
CREATE TABLE IF NOT EXISTS diary_attachment_mapping
(
    diary_date             INTEGER,
    referred_attachment_id INTEGER,

    FOREIGN KEY (diary_date) REFERENCES diary ("date"),
    FOREIGN KEY (referred_attachment_id) REFERENCES diary_attachment (id)
);""".split(";\n")
            statements.forEach {
                database.exec(it)
            }
        }

        fun getCharsCount(database: SQLite3, dateInt: Int): Int {
            val statement = database.compileStatement("SELECT length(content) FROM diary WHERE \"date\" IS ?", arrayOf(dateInt))
            val cursor = statement.cursor
            Assertion.doAssertion(cursor.step())
            val c = cursor.getInt(0)
            statement.release()
            return c
        }
    }
}