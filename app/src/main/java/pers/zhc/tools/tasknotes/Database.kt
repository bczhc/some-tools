package pers.zhc.tools.tasknotes

import pers.zhc.tools.MyApplication
import pers.zhc.tools.utils.*

class Database(path: String) : BaseDatabase(path) {
    init {
        checkAndConvertOldDatabase()
        initTable()
    }

    private fun initTable() {
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

    private fun checkAndConvertOldDatabase() {
        if (!db.hasTable("task_record")) return

        val hasOrderColumn = db.queryTableInfo("task_record").asSequence().map { it.name }.contains("order")
        if (!hasOrderColumn) {
            // old database
            val records = this.queryAll()
            db.exec("DROP TABLE task_record")
            initTable()
            this.batchInsert(records.asSequence())
        }
    }

    fun insert(record: Record) {
        val order = getTodayNextOrder()
        db.execBind(
            "INSERT INTO task_record (\"order\", description, mark, \"time\", creation_time) VALUES (?, ?, ?, ?, ?)",
            arrayOf(order, record.description, record.mark.enumInt, record.time.minuteOfDay, record.creationTime)
        )
    }

    fun delete(timestamp: Long) {
        db.execBind("DELETE FROM task_record WHERE creation_time IS ?", arrayOf(timestamp))
    }

    private fun batchInsert(records: Sequence<Record>) {
        db.withCompiledStatement(
            "INSERT INTO task_record (\"order\", description, mark, \"time\", creation_time) VALUES (?, ?, ?, ?, ?)"
        ) {
            db.beginTransaction()
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
            db.commit()
        }
    }

    fun reorderRecords(records: List<Record>) {
        db.withCompiledStatement("UPDATE task_record SET \"order\" = ? WHERE creation_time IS ?") {
            db.beginTransaction()
            records.forEachIndexed { index, record ->
                it.stepBind(arrayOf(index, record.creationTime))
            }
            db.commit()
        }
    }

    fun <R> withQueryAll(block: (rows: SQLiteRows<Record>) -> R): R {
        // note: the field `order` here with double quote, even when the table doesn't have
        // `order` column, this compiles with no error, and sqlite just ignores it. This is
        // a convenience for converting from an old version of database.
        return db.withQueriedRows(
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

    private fun getTodayNextOrder(): Int {
        val timestampRange = Time.getTodayTimestampRange()
        return db.queryOne(
            """SELECT max("order")
FROM task_record
WHERE creation_time BETWEEN ? AND ?""",
            arrayOf(timestampRange.first, timestampRange.last)
        ) { it.getInt(0) }!! + 1
    }

    fun queryToday(): List<Record> {
        val timestampRange = Time.getTodayTimestampRange()
        return db.withQueriedRows(
            """SELECT description, mark, "time", creation_time
FROM task_record
WHERE creation_time BETWEEN ? AND ?
ORDER BY "order"""",
            arrayOf(timestampRange.first, timestampRange.last),
            mapRow = {
                Record(
                    it.getText(0),
                    TaskMark.from(it.getInt(1))!!,
                    Time(it.getInt(2)),
                    it.getLong(3)
                )
            },
            block = { rows ->
                rows.asSequence().toList()
            },
        )
    }

    private fun queryAll(): List<Record> {
        return this.withQueryAll { it.asSequence().toList() }
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