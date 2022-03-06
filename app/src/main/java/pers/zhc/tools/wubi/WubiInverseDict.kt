package pers.zhc.tools.wubi

import android.content.Context
import pers.zhc.tools.utils.Common
import java.io.File

/**
 * @author bczhc
 */
class WubiInverseDictManager {
    companion object {
        lateinit var dictFile: File

        fun init(context: Context) {
            dictFile = Common.getInternalDatabaseFile(context, "wubi_inverse_dict")
        }

        fun openDatabase(): WubiInverseDictDatabase {
            return WubiInverseDictDatabase(dictFile.path)
        }

        fun useDatabase(f: (db: WubiInverseDictDatabase) -> Unit) {
            val database = openDatabase()
            f(database)
            database.close()
        }
    }
}