package pers.zhc.tools.charucd

import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.MyApplication
import pers.zhc.tools.utils.queryOne
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
}