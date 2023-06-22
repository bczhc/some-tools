package pers.zhc.tools.utils

import org.intellij.lang.annotations.Language
import pers.zhc.jni.sqlite.*
import kotlin.reflect.KClass

/**
 * @author bczhc
 */

fun <T> SQLite3.queryExec(
    @Language("SQLite") sql: String,
    binds: Array<Any>? = null,
    callback: (cursor: Cursor) -> T
): T {
    val statement = this.compileStatement(sql, binds ?: arrayOf())
    val cursor = statement.cursor
    val result = callback(cursor)
    statement.release()
    return result
}

/**
 * returns null if there's no such row
 */
fun <T> SQLite3.queryOne(
    @Language("SQLite") sql: String,
    binds: Array<Any>? = null,
    mapRow: (row: Cursor) -> T
): T? {
    return this.queryExec(sql, binds) {
        if (it.step()) {
            mapRow(it)
        } else {
            null
        }
    }
}

inline fun <T> KClass<SQLite3>.withNew(path: String, block: (db: SQLite3) -> T): T {
    val db = SQLite3.open(path)
    val r = block(db)
    db.close()
    return r
}

inline fun <R> SQLite3.withCompiledStatement(@Language("SQLite") sql: String, block: (statement: Statement) -> R): R {
    val statement = this.compileStatement(sql)
    val r = block(statement)
    statement.release()
    return r
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

fun <T> SQLite3.queryRows(
    @Language("SQLite") sql: String,
    binds: Array<Any>? = null,
    mapRow: (row: Cursor) -> T
): ArrayList<T> {
    val rows = this.queryMap(sql, binds ?: arrayOf(), mapRow)
    val list = ArrayList<T>().also { it.addAll(rows.asSequence()) }
    rows.release()
    return list
}

/**
 * [cmd] example: SELECT COUNT() FROM tbl_name
 */
fun SQLite3.getRowCount(@Language("SQLite") cmd: String, binds: Array<Any>? = null): Int {
    return this.queryOne(cmd, binds) { it.getInt(0) }!!
}

fun Statement.stepBind(binds: Array<Any>) {
    this.reset()
    this.bind(binds)
    this.step()
}

class SQLiteRows<T>(private val rows: Rows<T>, private val statement: Statement) : Iterator<T> {
    override fun hasNext() = rows.hasNext()
    override fun next(): T = rows.next()
    fun release() = statement.release()
}

fun <T> SQLite3.queryMap(
    @Language("SQLite") sql: String,
    binds: Array<Any> = arrayOf(),
    mapRow: (row: Cursor) -> T
): SQLiteRows<T> {
    val statement = this.compileStatement(sql, binds)
    val rows = statement.queryRows(mapRow)
    return SQLiteRows(rows, statement)
}

fun SQLite3.getTables(): List<String> {
    return this.querySchema().filter { it.type == Schema.Type.TABLE }.map { it.tableName }
}

fun <T, R> SQLite3.withQueriedRows(
    @Language("SQLite") sql: String,
    binds: Array<Any>? = null,
    block: (rows: SQLiteRows<T>) -> R,
    mapRow: (row: Cursor) -> T
): R {
    val rows = this.queryMap(sql, binds ?: emptyArray(), mapRow)
    val r = block(rows)
    rows.release()
    return r
}

fun <T> SQLite3.queryAdd(
    @Language("SQLite") sql: String,
    binds: Array<Any>? = null,
    collection: MutableCollection<T>,
    mapRow: (row: Cursor) -> T
) {
    this.withQueriedRows(sql, binds, { collection.addAll(it.asSequence()) }, mapRow)
}

fun <T> SQLite3.withTransaction(block: (db: SQLite3) -> T): T {
    this.beginTransaction()
    val r = block(this)
    this.commit()
    return r
}

/**
 * returns null if there's no such row
 */
fun <T> Statement.queryOne(binds: Array<Any>?, mapRow: (row: Cursor) -> T): T? {
    this.reset()
    this.bind(binds)
    val rows = this.queryRows(mapRow)
    if (!rows.hasNext()) return null
    return rows.next()
}
