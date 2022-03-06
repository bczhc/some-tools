package pers.zhc.tools.wubi

import android.content.Context
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.FileUtil
import pers.zhc.tools.wubi.WubiIME.checkCodeOrThrow
import java.io.File

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

    /**
     * @throws IllegalArgumentException for illegal wubi code
     */
    fun addRecord(word: String, code: String) {
        checkCodeOrThrow(code)

        val candidates = fetchCandidates(code)
        if (candidates == null) {
            // there's no wubi code record in the dictionary database
            // we need to add an initial record
            database.execBind(
                "INSERT INTO wubi_code_${code[0]} (code, word) VALUES (?, ?)",
                arrayOf(code, "")
            )
            updateRecord(listOf(word), code)
            return
        }
        val list = candidates.toMutableList()
        list.add(word)

        updateRecord(list, code)
    }

    fun updateRecord(candidates: List<String>, code: String) {
        checkCodeOrThrow(code)

        database.execBind(
            "UPDATE wubi_code_${code[0]} SET word=? WHERE code IS ?",
            arrayOf(candidates.joinToString("|"), code)
        )
    }

    /**
     * Delete the entire wubi code record
     *
     * When the last candidate word in a record has been removed, the record needs to be deleted also.
     * @throws IllegalArgumentException for illegal wubi code
     */
    fun deleteCodeRecord(code: String) {
        checkCodeOrThrow(code)

        database.execBind(
            "DELETE FROM wubi_code_${code[0]} WHERE code IS ?",
            arrayOf(code)
        )
    }

    companion object {
        lateinit var databasePath: String
        private var lazyDB = lazy { DictionaryDatabase(databasePath) }

        val dictDatabase: DictionaryDatabase
            get() = lazyDB.value

        fun init(context: Context) {
            databasePath = Common.getInternalDatabaseDir(context, "wubi_code.db").path
        }

        /**
         * copy and substitute the inner database file
         */
        fun changeDatabase(path: String) {
            if (lazyDB.isInitialized()) {
                lazyDB.value.database.close()
            }
            FileUtil.copy(File(path), File(databasePath))
            lazyDB = lazy { DictionaryDatabase(databasePath) }
        }
    }
}
