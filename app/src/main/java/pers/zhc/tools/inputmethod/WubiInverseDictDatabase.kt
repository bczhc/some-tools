package pers.zhc.tools.inputmethod

import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.util.Assertion

/**
 * @author bczhc
 */
class WubiInverseDictDatabase(path: String) {
    private val database = SQLite3.open(path)

    init {
        database.exec(
            """CREATE TABLE IF NOT EXISTS dict
(
    word TEXT NOT NULL,
    code TEXT NOT NULL
)"""
        )

        database.exec(
            """CREATE TABLE IF NOT EXISTS update_info
(
    -- when wubi_dict updated, the mark will be set to true, and then regenerate the database when needed
    update_mark INTEGER NOT NULL PRIMARY KEY
)"""
        )
        checkAndInsertUpdateMark()
    }

    fun query(word: String): Array<String> {
        val result = ArrayList<String>()

        val statement = database.compileStatement("SELECT code FROM dict WHERE word IS ?", arrayOf(word))
        val cursor = statement.cursor
        while (cursor.step()) {
            result.add(cursor.getText(0))
        }
        statement.release()

        return result.toArray(Array(0) { "" })
    }

    fun update(wubiDictDatabase: SQLite3) {
        database.beginTransaction()
        @Suppress("SqlWithoutWhere")
        database.exec("DELETE FROM dict")
        val insertStmt = database.compileStatement("INSERT INTO dict (word, code) VALUES (?, ?)")

        for (c in 'a'..'z') {
            val statement = wubiDictDatabase.compileStatement("SELECT code, word FROM wubi_code_$c")
            val cursor = statement.cursor
            while (cursor.step()) {
                val code = cursor.getText(0)
                val word = cursor.getText(1)
                word.split('|').forEach {
                    insertStmt.reset()
                    insertStmt.bindText(1, it)
                    insertStmt.bindText(2, code)
                    insertStmt.step()
                }
            }
            statement.release()
        }

        insertStmt.release()

        updateUpdateMark(false)
        database.commit()
    }

    fun checkUpdate(): Boolean {
        val statement = database.compileStatement("SELECT update_mark FROM update_info")
        val cursor = statement.cursor
        Assertion.doAssertion(cursor.step())
        val r = cursor.getInt(0)
        statement.release()
        return r != 0
    }

    private fun checkAndInsertUpdateMark() {
        if (database.getRecordCount("update_info") == 0) {
            database.exec("INSERT INTO update_info VALUES (TRUE)")
        }
    }

    fun updateUpdateMark(update: Boolean) {
        database.execBind(
            "UPDATE update_info SET update_mark=?", arrayOf(
                if (update) {
                    1
                } else {
                    0
                }
            )
        )
    }

    fun close() {
        database.close()
    }
}