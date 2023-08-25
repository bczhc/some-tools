package pers.zhc.tools.fdb

import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.withCompiledStatement
import pers.zhc.tools.utils.withNew
import java.io.File
import java.io.FileInputStream

/**
 * @author bczhc
 */
enum class PathVersion(val versionName: String) {
    /**
     * initial version; no magic number
     * doc: 3e70c15:app/src/main/java/pers/zhc/tools/floatingdrawing/PaintView.java:464
     */
    VERSION_1_0("1.0"),

    VERSION_2_0("2.0"),

    /**
     * doc: b577df3:app/src/main/java/pers/zhc/tools/floatingdrawing/PaintView.java:369
     */
    VERSION_2_1("2.1"),

    /**
     * start using SQLite3 database
     * doc: 3631bf8:app/src/main/java/pers/zhc/tools/floatingdrawing/PaintView.java:1181
     */
    VERSION_3_0("3.0"),

    /**
     * multi-layer path import
     * doc: fda72ba:app/src/main/java/pers/zhc/tools/fdb/PathSaver.java:19
     */
    VERSION_3_1("3.1"),

    /**
     * use packed bytes as stroke info heads
     * doc: 9e52afe:app/src/main/java/pers/zhc/tools/fdb/PathSaver.java:18
     */
    VERSION_4_0("4.0"),

    Unknown("Unknown");

    companion object {
        fun getPathVersion(f: File): PathVersion {
            var version: PathVersion? = null

            // check paths that use SQLite database (since path 3.0)
            val db = SQLite3.open(f.path)
            if (!db.checkIfCorrupt()) {
                db.withCompiledStatement("SELECT version FROM info") {
                    val cursor = it.cursor
                    version = if (cursor.step()) {
                        val versionString = cursor.getText(0)
                        mapOf(
                            Pair("3.0", VERSION_3_0),
                            Pair("3.1", VERSION_3_1),
                            Pair("4.0", VERSION_4_0)
                        )[versionString] ?: Unknown
                    } else {
                        Unknown
                    }
                }
            }
            db.close()

            if (version != null) {
                if (version == VERSION_3_0) {
                    version = checkMore(f.path)
                }
                return version!!
            }

            // check path versions 1.0, 2.0, 2.1
            val inputStream = FileInputStream(f)
            val buf = ByteArray(12)
            Common.doAssertion(inputStream.read(buf) == 12)
            version = if (buf.contentEquals("path ver 2.0".toByteArray())) {
                VERSION_2_0
            } else if (buf.contentEquals("path ver 2.1".toByteArray())) {
                VERSION_2_1
            } else {
                VERSION_1_0
            }
            inputStream.close()

            return version!!
        }

        // now some path 3.0 are actually path 3.1, due to some negligence of mine for not controlling
        // the versions well
        /**
         * [path] is path 3.0
         */
        private fun checkMore(path: String): PathVersion {
            SQLite3::class.withNew(path) { db ->
                // the path file has been recognized to be path 3.0, so extraInfo must be valid
                val extraInfo = ExtraInfo.getExtraInfo(db)!!
                if (extraInfo.layersInfo != null) {
                    // the path is actually path 3.1 (with layers info)
                    return VERSION_3_1
                }
            }

            return VERSION_3_0
        }
    }
}
