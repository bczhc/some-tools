package pers.zhc.tools.fdb

import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.utils.queryMap
import java.io.File

typealias ProgressCallback = (progress: Float, phase: PathProcessor.ProgressPhase) -> Unit

object PathProcessor {
    private class Record(
        val mark: Int,
        val info: ByteArray,
        val x: Double,
        val y: Double,
    )

    private class Path {
        val records = ArrayList<Record>()
    }

    enum class ProgressPhase(val number: Int) {
        /**
         * optimize undo operations
         */
        PHASE1(1),

        /**
         * write to database
         */
        PHASE2(2),
        DONE(0)
    }

    /**
     * strip unneeded touch points due to "undo"
     */
    fun optimizePath(pathFile: File, table: String, progressCallback: ProgressCallback? = null) {
        val db = SQLite3.open(pathFile.path)
        val recordCount = db.getRecordCount(table)

        val undoList = ArrayList<Path>()
        val redoList = ArrayList<Path>()

        var path: Path? = null

        var counter = 0

        val rows = db.queryMap("SELECT mark, info, x, y FROM $table") { it }
        for (cursor in rows) {
            val mark = cursor.getInt(0)
            val info = cursor.getBlob(1)
            val x = cursor.getDouble(2)
            val y = cursor.getDouble(3)

            when (mark) {
                0x01, 0x11 -> {
                    // down
                    path = Path().also {
                        it.records.add(Record(mark, info, x, y))
                    }
                }

                0x02, 0x12 -> {
                    // move
                    path?.records?.add(Record(mark, info, x, y))
                }

                0x03, 0x13 -> {
                    // up
                    path?.records?.add(Record(mark, info, x, y))
                    path?.let { undoList.add(it) }
                }

                0x20 -> {
                    // undo
                    if (undoList.isNotEmpty()) {
                        val last = undoList.removeLast()
                        redoList.add(last)
                    }
                }

                0x30 -> {
                    // redo
                    if (redoList.isNotEmpty()) {
                        val last = redoList.removeLast()
                        undoList.add(last)
                    }
                }

                else -> {
                    // not expected
                }
            }
            ++counter
            progressCallback?.invoke(counter.toFloat() / recordCount.toFloat(), ProgressPhase.PHASE1)
        }
        rows.release()

        @Suppress("SqlWithoutWhere")
        db.exec("DELETE FROM $table")
        db.beginTransaction()
        val statement = db.compileStatement("INSERT INTO $table (mark, info, x, y) VALUES(?, ?, ?, ?)")

        undoList.asSequence().map { it.records }.flatten().forEachIndexed { index, it ->
            statement.reset()
            statement.bind(1, it.mark)
            statement.bindBlob(2, it.info)
            statement.bind(3, it.x)
            statement.bind(4, it.y)
            statement.step()

            progressCallback?.invoke(index.toFloat() / undoList.size.toFloat(), ProgressPhase.PHASE2)
        }

        db.commit()
        statement.release()

        db.close()

        progressCallback?.invoke(0F, ProgressPhase.DONE)
    }
}