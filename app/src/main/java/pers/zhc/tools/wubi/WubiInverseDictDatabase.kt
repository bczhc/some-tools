package pers.zhc.tools.wubi

import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.CodepointIterator
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

    fun composeCodeFromWord(word: String): String? {
        val findLongestCode = { queries: Array<String> ->
            var index = 0
            var length = 0
            queries.forEachIndexed { i, s ->
                if (s.length > length) {
                    index = i
                    length = s.length
                }
            }
            queries[index]
        }
        val queryChars = fun(codepoints: List<Int>): List<String>? {
            val queries = codepoints.map {
                val char = JNI.Utf8.codepoint2str(it)!!
                query(char)
            }
            queries.forEach {
                if (it.isEmpty()) {
                    // if one of the characters cannot be found its corresponding Wubi code, return null, meaning that
                    // the user should enter the Wubi code manually
                    return null
                }
            }

            return queries.map {
                val longestCode = findLongestCode(it)
                if (longestCode.length < 2) {
                    // Wubi code composition requires full code
                    return null
                }
                longestCode
            }
        }

        val codepoints = CodepointIterator(word).asSequence().toList()

        when (codepoints.size) {
            1 -> {
                // if the single character already exists in the Wubi dictionary, there's no need to compose code from
                // the method; if not, just add the new character, no need for the method's result
                return null
            }
            2 -> {
                // take: xx-- xx--
                val queries = queryChars(codepoints) ?: return null
                return "${queries[0].substring(0, 2)}${queries[1].substring(0, 2)}"
            }
            3 -> {
                // take: x--- x--- xx--
                val queries = queryChars(codepoints) ?: return null
                return "${queries[0][0]}${queries[1][0]}${queries[2].substring(0, 2)}"
            }
            else -> {
                if (codepoints.size >= 4) {
                    // multi-character words
                    // take: x--- x--- x--- ---- ... ---- x---
                    val toke = codepoints.take(3).toMutableList().also { it.add(codepoints.last()) }
                    val queries = queryChars(toke) ?: return null
                    return "${queries[0][0]}${queries[1][0]}${queries[2][0]}${queries[3][0]}"
                }
            }
        }
        // no composing solution, enter the word's Wubi code manually
        return null
    }

    fun checkExistence(word: String): Boolean {
        return database.hasRecord("SELECT word FROM dict WHERE word IS ?", arrayOf(word))
    }

    fun close() {
        database.close()
    }
}