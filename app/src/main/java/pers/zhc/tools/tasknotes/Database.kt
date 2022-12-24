package pers.zhc.tools.tasknotes

import pers.zhc.tools.MyApplication
import pers.zhc.tools.utils.*

class Database(path: String) : BaseDatabase(path) {
    init {
        db.exec(
            """CREATE TABLE IF NOT EXISTS task_record
(
    "order"       INTEGER NOT NULL,
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
            "INSERT INTO task_record (\"order\", description, mark, \"time\", creation_time) VALUES (?, ?, ?, ?, ?)",
            arrayOf(0, record.description, record.mark.enumInt, record.time.minuteOfDay, record.creationTime)
        )
    }

    fun delete(timestamp: Long) {
        db.execBind("DELETE FROM task_record WHERE creation_time IS ?", arrayOf(timestamp))
    }

    private fun batchDelete(timestamp: Sequence<Long>) {
        db.withCompiledStatement("DELETE FROM task_record WHERE creation_time IS ?") {
            timestamp.forEach { timestamp ->
                it.stepBind(arrayOf(timestamp))
            }
        }
    }

    private fun batchAdd(records: Sequence<Record>) {
        db.withCompiledStatement(
            "INSERT INTO task_record (\"order\", description, mark, \"time\", creation_time) VALUES (?, ?, ?, ?, ?)"
        ) {
            records.forEachIndexed { index, record ->
                it.stepBind(
                    arrayOf(
                        index,
                        record.description,
                        record.mark.enumInt,
                        record.time.minuteOfDay,
                        record.creationTime
                    )
                )
            }
        }
    }

    fun reorderRecords(records: List<Record>) {
        batchDelete(records.asSequence().map { it.creationTime })
        batchAdd(records.asSequence())
    }

    fun withQueryAll(block: (rows: SQLiteRows<Record>) -> Unit) {
        db.withQueriedRows(
            "SELECT description, mark, \"time\", creation_time FROM task_record ORDER BY \"order\"",
            block = block,
            mapRow = {
                Record(
                    it.getText(0),
                    TaskMark.from(it.getInt(1))!!,
                    Time(it.getInt(2)),
                    it.getLong(3)
                )
            }
        )
    }

    fun query(creationTime: Long): Record? {
        return db.queryOne(
            """SELECT description, mark, "time", creation_time
FROM task_record
WHERE creation_time IS ?""",
            arrayOf(creationTime)
        ) {
            Record(
                it.getText(0),
                TaskMark.from(it.getInt(1))!!,
                Time(it.getInt(2)),
                it.getLong(3)
            )
        }
    }

    fun update(creationTime: Long, record: Record) {
        db.execBind(
            """UPDATE task_record
SET description=?,
    mark=?,
    "time"=?,
    creation_time=?
WHERE creation_time IS ?""",
            arrayOf(
                record.description, record.mark.enumInt, record.time.minuteOfDay, record.creationTime,
                creationTime
            )
        )
    }

    companion object {
        private val databasePath by lazy {
            Common.getInternalDatabaseFile(MyApplication.appContext, "task_notes")
        }

        val database by lazy { Database(databasePath.path) }
    }
}