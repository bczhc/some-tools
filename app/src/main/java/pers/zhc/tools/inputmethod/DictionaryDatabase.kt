package pers.zhc.tools.inputmethod

import android.content.Context
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.utils.Common

/**
 * @author bczhc
 */
class DictionaryDatabase private constructor(path: String) {
    val database = SQLite3.open(path)

    /**
     * Fetch wubi candidate words.
     *
     * @param wubiCodeStr      wubi code
     * @return fetched candidates string array, null if not found the specified wubi code (no such column or table)
     * @throws RuntimeException sqlite error
     */
    fun fetchCandidates(wubiCodeStr: String): Array<String>? {
        if (wubiCodeStr.isEmpty()) return null
        var r: Array<String>? = null
        try {
            val tableName = "wubi_code_${wubiCodeStr[0]}"
            val statement = database.compileStatement("SELECT word FROM $tableName WHERE code IS ?")
            statement.bindText(1, wubiCodeStr)
            val cursor = statement.cursor
            if (cursor.step()) {
                val selected = cursor.getText(0)
                r = selected.split(Regex("\\|")).toTypedArray()
            }
            statement.release()
        } catch (e: Exception) {
            if (e.toString().contains("no such column") ||
                e.toString().contains("no such table")
            ) {
                return null
            }
            throw RuntimeException(e)
        }
        return r
    }

    private fun checkCode(code: String) {
        if (code.isEmpty()) throw IllegalArgumentException()
        if (code[0] !in 'a'..'z') throw IllegalArgumentException()
    }

    /**
     * @throws IllegalArgumentException for illegal wubi code
     */
    fun addRecord(word: String, code: String) {
        checkCode(code)

        val candidates = fetchCandidates(code)!!
        val list = candidates.toMutableList()
        list.add(word)

        updateRecord(list, code)
    }

    fun updateRecord(candidates: List<String>, code: String) {
        checkCode(code)

        database.execBind(
            "UPDATE wubi_code_${code[0]} SET word=? WHERE code IS ?",
            arrayOf(candidates.joinToString("|"), code)
        )
    }

    companion object {
        lateinit var databasePath: String
        private var dictDatabase: DictionaryDatabase? = null

        fun getDatabaseRef(): DictionaryDatabase {
            if (dictDatabase == null) {
                dictDatabase = DictionaryDatabase(databasePath)
            }
            return dictDatabase!!
        }

        fun init(context: Context) {
            databasePath = Common.getInternalDatabaseDir(context, "wubi_code.db").path
        }
    }
}
