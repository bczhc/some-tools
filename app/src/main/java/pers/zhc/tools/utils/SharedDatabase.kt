package pers.zhc.tools.utils

import pers.zhc.jni.sqlite.SQLite3

/**
 * @author bczhc
 */
class SharedDatabase(val path: String): SharedRef<SQLite3>() {
    override fun create(): SQLite3 {
        return SQLite3.open(path)
    }

    override fun close(obj: SQLite3) {
        obj.close()
    }
}