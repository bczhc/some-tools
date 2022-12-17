package pers.zhc.tools.diary

import org.intellij.lang.annotations.Language
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.MyApplication
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.androidAssert
import pers.zhc.tools.utils.rc.Ref
import pers.zhc.tools.utils.rc.ReusableRcManager
import pers.zhc.tools.utils.withCompiledStatement

/**
 * @author bczhc
 * TODO: avoid directly operating with this [database] inner [SQLite3] object
 */
class DiaryDatabase(path: String) {
    val database = SQLite3.open(path)

    init {
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

    fun getCharsCount(dateInt: Int): Int {
        return database.withCompiledStatement("SELECT length(content) FROM diary WHERE \"date\" IS ?") {
            it.bind(arrayOf(dateInt))
            val cursor = it.cursor
            androidAssert(cursor.step())
            cursor.getInt(0)
        }
    }

    private fun close() {
        database.close()
    }

    companion object {
        val internalDatabasePath by lazy {
            Common.getInternalDatabaseDir(MyApplication.appContext, "diary.db")
        }

        private val databaseManager = object : ReusableRcManager<DiaryDatabase>() {
            override fun create(): DiaryDatabase {
                return DiaryDatabase(internalDatabasePath.path)
            }

            override fun release(obj: DiaryDatabase) {
                obj.close()
            }
        }

        fun getDatabaseRef(): Ref<DiaryDatabase> {
            return databaseManager.getRefOrCreate()
        }

        fun getDatabaseRefCount(): Int {
            return databaseManager.getRefCount()
        }
    }
}