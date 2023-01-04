package pers.zhc.tools.diary

import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.MyApplication
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.fromJsonOrNull
import pers.zhc.tools.utils.queryOne
import pers.zhc.tools.utils.withNew

object LocalInfoDatabase {
    private fun <T> withDatabase(block: (SQLite3) -> T): T {
        return SQLite3::class.withNew(databasePath.path) {
            initDatabase(it)
            block(it)
        }
    }

    private fun initDatabase(db: SQLite3) {
        db.exec("CREATE TABLE IF NOT EXISTS info (json TEXT NOT NULL)")
    }

    fun getInfo(): LocalInfo? {
        val json = withDatabase {
            it.queryOne("SELECT json FROM info") { c -> c.getText(0) }
        } ?: return null
        return MyApplication.GSON.fromJsonOrNull(json, LocalInfo::class.java)
    }

    fun updateInfo(info: LocalInfo) {
        val json = MyApplication.GSON.toJson(info)
        withDatabase {
            @Suppress("SqlWithoutWhere")
            it.exec("DELETE FROM info")
            it.execBind("INSERT INTO info VALUES (?)", arrayOf(json))
        }
    }

    private val databasePath by lazy {
        Common.getInternalDatabaseFile(MyApplication.appContext, "diary_local_info")
    }
}