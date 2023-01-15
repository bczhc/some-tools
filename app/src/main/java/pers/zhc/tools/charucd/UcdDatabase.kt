package pers.zhc.tools.charucd

import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.MyApplication
import pers.zhc.tools.utils.queryOne
import pers.zhc.tools.utils.queryRows
import java.io.File

/**
 * @author bczhc
 */
class UcdDatabase(val file: File) {
    private val database = SQLite3.open(file.path)
    private val queryStatement = database.compileStatement("SELECT json FROM ucd WHERE codepoint IS ?")

    fun close() {
        queryStatement.release()
        database.close()
    }

    fun query(codepoint: Int): Properties? {
        val properties = queryStatement.queryOne(arrayOf(codepoint)) {
            it.getText(0)
        } ?: return null
        return MyApplication.GSON.fromJson(properties, Properties::class.java)
    }

    data class NameLookupResult(
        val codepoint: Int,
        val na: String,
        val na1: String,
        val alias: String?,
    )

    fun queryByNameLike(name: String, limit: Int): List<NameLookupResult> {
        return database.queryRows(
            """SELECT codepoint, na, na1, alias
FROM ucd
WHERE na LIKE ?1
   OR na1 LIKE ?1
   OR alias LIKE ?1
LIMIT ?2""",
            arrayOf("%$name%", limit)
        ) {
            NameLookupResult(
                it.getInt(0),
                it.getText(1),
                it.getText(2),
                it.getText(3),
            )
        }.toList()
    }

    companion object {
        val databaseFile by lazy {
            File(MyApplication.appContext.filesDir, "ucd.db")
        }

        fun openDatabase(): UcdDatabase {
            return UcdDatabase(databaseFile)
        }

        fun <T> useDatabase(block: (UcdDatabase) -> T): T {
            val db = UcdDatabase(databaseFile)
            val result = block(db)
            db.close()
            return result
        }
    }
}