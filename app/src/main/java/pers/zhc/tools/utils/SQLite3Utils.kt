package pers.zhc.tools.utils

import org.intellij.lang.annotations.Language
import pers.zhc.jni.sqlite.Cursor
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.jni.sqlite.Statement
import kotlin.reflect.KClass

/**
 * @author bczhc
 */

fun SQLite3.queryExec(@Language("SQLite") sql: String, binds: Array<Any>? = null, callback: (cursor: Cursor) -> Unit) {
    val statement = this.compileStatement(sql, binds ?: arrayOf())
    val cursor = statement.cursor
    callback(cursor)
    statement.release()
}

inline fun KClass<SQLite3>.withNew(path: String, block: (db: SQLite3) -> Unit) {
    val db = SQLite3.open(path)
    block(db)
    db.close()
}

inline fun SQLite3.withCompiledStatement(@Language("SQLite") sql: String, block: (statement: Statement) -> Unit) {
    val statement = this.compileStatement(sql)
    block(statement)
    statement.release()
}

fun Statement.execute(binds: Array<Any>) {
    this.reset()
    this.bind(binds)
    this.step()
}

fun Statement.execute() {
    this.reset()
    this.step()
}

fun <T> Cursor.collectRows(f: (row: Cursor) -> T): ArrayList<T> {
    val list = ArrayList<T>()
    while (this.step()) {
        list.add(f(this))
    }
    return list
}

fun <T> SQLite3.queryRows(@Language("SQLite") sql: String, binds: Array<Any>? = null, mapRow: (row: Cursor) -> T): ArrayList<T> {
    var collected: ArrayList<T>? = null
    this.queryExec(sql, binds) {
        collected = it.collectRows(mapRow)
    }
    return collected!!
}
