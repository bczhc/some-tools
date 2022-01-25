package pers.zhc.tools.charucd

import org.json.JSONArray
import pers.zhc.jni.sqlite.SQLite3
import java.io.File

/**
 * @author bczhc
 */
class UcdDatabase(val file: File) {
    private val database = SQLite3.open(file.path)
    private val queryStatement = database.compileStatement("SELECT properties FROM ucd WHERE codepoint IS ?")

    fun close() {
        queryStatement.release()
        database.close()
    }

    fun query(codepoint: Int): JSONArray? {
        queryStatement.reset()
        queryStatement.bind(1, codepoint)
        val cursor = queryStatement.cursor
        return if (cursor.step()) {
            JSONArray(cursor.getText(0))
        } else null
    }
}