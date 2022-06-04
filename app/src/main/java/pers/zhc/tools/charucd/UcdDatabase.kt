package pers.zhc.tools.charucd

import com.google.gson.JsonObject
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.MyApplication
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

    fun query(codepoint: Int): JsonObject? {
        queryStatement.reset()
        queryStatement.bind(1, codepoint)
        val cursor = queryStatement.cursor
        return if (cursor.step()) {
            MyApplication.defaultGson.fromJson(cursor.getText(0), JsonObject::class.java)
        } else null
    }
}