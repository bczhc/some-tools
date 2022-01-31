package pers.zhc.tools.utils

import org.intellij.lang.annotations.Language
import pers.zhc.jni.sqlite.Cursor
import pers.zhc.jni.sqlite.SQLite3

/**
 * @author bczhc
 */

fun SQLite3.queryExec(@Language("SQLite") sql: String, binds: Array<Any>? = null, callback: (cursor: Cursor) -> Unit) {
    val statement = this.compileStatement(sql, binds ?: arrayOf())
    val cursor = statement.cursor
    callback(cursor)
    statement.release()
}
