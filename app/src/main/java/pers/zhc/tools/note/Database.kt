package pers.zhc.tools.note

import pers.zhc.jni.sqlite.Cursor
import pers.zhc.tools.MyApplication
import pers.zhc.tools.utils.*
import pers.zhc.tools.utils.rc.Ref
import pers.zhc.tools.utils.rc.ReusableRcManager

class Database(path: String) : BaseDatabase(path) {
    init {
        db.exec(
            """CREATE TABLE IF NOT EXISTS doc
(
    -- timestamp
    t       INTEGER NOT NULL PRIMARY KEY,
    title   TEXT    NOT NULL,
    content TEXT    NOT NULL
)"""
        )
    }

    fun addRecord(timestamp: Long = System.currentTimeMillis(), title: String, content: String) {
        db.execBind("INSERT INTO doc (t, title, content) VALUES (?, ?, ?)", arrayOf(timestamp, title, content))
    }

    private fun mapRow(cursor: Cursor): Record {
        return Record(
            cursor.getLong(0),
            cursor.getText(1),
            cursor.getText(2)
        )
    }

    fun queryAll(): ArrayList<Record> {
        return db.queryRows("SELECT t, title, content FROM doc", mapRow = this::mapRow)
    }

    fun query(timestamp: Long): Record? {
        var record: Record? = null
        db.queryExec("SELECT t, title, content FROM doc WHERE t IS ?", arrayOf(timestamp)) {
            if (it.step()) {
                record = mapRow(it)
            }
        }
        return record
    }

    fun deleteRecord(timestamp: Long) {
        db.execBind("DELETE FROM doc WHERE t IS ?", arrayOf(timestamp))
    }

    fun update(timestamp: Long, newRecord: Record) {
        db.execBind(
            "UPDATE doc SET title = ?, content = ? WHERE t IS ?",
            arrayOf(newRecord.time, newRecord.content, timestamp)
        )
    }

    companion object {
        val databasePath by lazy {
            Common.getInternalDatabaseFile(MyApplication.appContext, "notes")
        }

        private val sharedDbManager = object : ReusableRcManager<Database>() {
            override fun create(): Database {
                return Database(databasePath.path)
            }

            override fun release(obj: Database) {
                obj.close()
            }
        }

        fun getDbRef(): Ref<Database> {
            return sharedDbManager.getRefOrCreate()
        }
    }
}