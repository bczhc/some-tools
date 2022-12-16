package pers.zhc.tools.tasknotes

import pers.zhc.tools.MyApplication
import pers.zhc.tools.utils.BaseDatabase
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.withCompiledStatement

class Database(path: String) : BaseDatabase(path) {
    init {
        db.exec(
            """CREATE TABLE IF NOT EXISTS task_record
(
    description   TEXT    NOT NULL,
    -- 0: start; 1: end
    mark          INTEGER NOT NULL,
    -- time (minute of day) = hour * 60 + minute
    "time"        INTEGER,
    creation_time INTEGER NOT NULL PRIMARY KEY
)
"""
        )
    }

    fun insert(record: Record) {
        db.execBind(
            "INSERT INTO task_record (description, mark, \"time\", creation_time) VALUES (?, ?, ?, ?)",
            arrayOf(record.description, record.mark.enumInt, record.time.minuteOfDay, record.creationTime)
        )
    }

    fun delete(timestamp: Long) {
        db.execBind("DELETE FROM task_record WHERE creation_time IS ?", arrayOf(timestamp))
    }

    fun queryAll(): ArrayList<Record> {
        return db.withCompiledStatement("SELECT description, mark, \"time\", creation_time FROM task_record") {
            val cursor = it.cursor
            val records = ArrayList<Record>()
            while (cursor.step()) {
                records.add(
                    Record(
                        cursor.getText(0),
                        TaskMark.from(cursor.getInt(1))!!,
                        Time(cursor.getInt(2)),
                        cursor.getLong(3)
                    )
                )
            }
            records
        }
    }

    companion object {
        private val databasePath by lazy {
            Common.getInternalDatabaseFile(MyApplication.appContext, "task_notes")
        }

        val database by lazy { Database(databasePath.path) }
    }
}