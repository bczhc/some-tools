package pers.zhc.tools.utils

import pers.zhc.jni.sqlite.SQLite3

abstract class BaseDatabase(path: String) {
    protected val db = SQLite3.open(path)
    protected open fun close() {
        db.close()
    }

    fun inner(): SQLite3 {
        return db
    }

    companion object {
        class SharedDatabase(private val path: String) : SharedRef<BaseDatabase>() {
            override fun create(): BaseDatabase {
                return object : BaseDatabase(path) {}
            }

            override fun close(obj: BaseDatabase) {
                obj.close()
            }
        }
    }
}
