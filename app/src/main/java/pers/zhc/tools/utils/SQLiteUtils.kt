package pers.zhc.tools.utils

import org.intellij.lang.annotations.Language
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.util.Assertion

/**
 * @author bczhc
 */

/**
 * [cmd] example: SELECT COUNT() FROM tbl_name
 */
fun SQLite3.getRowCount(@Language("SQLite") cmd: String): Int {
    val statement = this.compileStatement(cmd)
    val cursor = statement.cursor
    Assertion.doAssertion(cursor.step())
    val count = cursor.getInt(0)
    statement.release()
    return count
}