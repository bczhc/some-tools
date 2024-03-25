package pers.zhc.tools.note

import pers.zhc.jni.sqlite.Cursor
import pers.zhc.tools.MyApplication
import pers.zhc.tools.utils.*
import pers.zhc.tools.utils.rc.Ref
import pers.zhc.tools.utils.rc.ReusableRcManager
import java.io.File

class Database(val path: String) : BaseDatabase(path) {
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

    fun addRecord(record: Record) {
        addRecord(record.time, record.title, record.content)
    }

    private fun mapRow(cursor: Cursor): Record {
        return Record(
            cursor.getLong(0),
            cursor.getText(1),
            cursor.getText(2)
        )
    }

    fun queryExec(callback: (row: Record) -> Unit) {
        db.queryExec("SELECT t, title, content FROM doc ORDER BY t") { c ->
            while (c.step()) {
                val row = mapRow(c)
                callback(row)
            }
        }
    }

    fun queryAll(): ArrayList<Record> {
        return db.queryRows("SELECT t, title, content FROM doc ORDER BY t", mapRow = this::mapRow)
    }

    fun recordCount(): Int {
        return db.getRecordCount("doc")
    }

    fun query(timestamp: Long): Record? {
        var record: Record? = null
        db.queryExec("SELECT t, title, content FROM doc WHERE t IS ? ORDER BY t", arrayOf(timestamp)) {
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
            arrayOf(newRecord.title, newRecord.content, timestamp)
        )
    }

    fun batchDelete(timestamps: Iterator<Long>) {
        db.beginTransaction()
        db.withCompiledStatement("DELETE FROM doc WHERE t IS ?") {
            for (timestamp in timestamps) {
                it.execute(arrayOf(timestamp))
            }
        }
        db.commit()
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

        fun getRefCount(): Int {
            return sharedDbManager.getRefCount()
        }

        fun joinTwo(db1Path: File, db2Path: File, outPath: File) {
            val db1 = Database(db1Path.path)
            val db2 = Database(db2Path.path)
            val out = Database(outPath.path)

            out.db.beginTransaction()

            if (out.recordCount() != 0) {
                throw RuntimeException("An empty 'out' database is required")
            }
            val timestamps = HashSet<Long>()
            val add = { row: Record ->
                if (!timestamps.contains(row.time)) {
                    timestamps.add(row.time)
                    out.addRecord(row)
                }
            }

            db1.queryExec { add(it) }
            db2.queryExec { add(it) }

            out.db.commit()
            out.close()
            db1.close()
            db2.close()
        }
    }
}
