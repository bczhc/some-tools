package pers.zhc.tools.diary

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import org.intellij.lang.annotations.Language
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.DigestUtil
import pers.zhc.tools.utils.sqlite.SQLite3
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * @author bczhc
 */
open class DiaryBaseActivity : BaseActivity() {
    companion object {
        var diaryDatabaseRef: DiaryDatabaseRef = DiaryDatabaseRef()

        @JvmStatic
        protected fun initDatabase(database: SQLite3) {
            @Language("SQLite") val statements =
                """PRAGMA foreign_keys = ON;
-- main diary content table
CREATE TABLE IF NOT EXISTS diary
(
    "date"  INTEGER PRIMARY KEY,
    content TEXT NOT NULL
);
-- diary attachment file info table
-- identifier: SHA1(hex(file).concat(packIntLittleEndian(file.length)))
CREATE TABLE IF NOT EXISTS diary_attachment_file
(
    identifier         TEXT NOT NULL PRIMARY KEY,
    -- Enum:
    -- RAW 0
    -- TEXT 1
    -- IMAGE 2
    -- AUDIO 3
    storage_type       INTEGER,
    addition_timestamp INTEGER UNIQUE,
    filename           TEXT,
    description        TEXT NOT NULL
);
-- diary attachment text storage table
CREATE TABLE IF NOT EXISTS diary_attachment_text
(
    identifier TEXT NOT NULL PRIMARY KEY,
    content    TEXT NOT NULL,

    FOREIGN KEY (identifier) REFERENCES diary_attachment_file (identifier)
);
-- diary attachment file reference table; an attachment can have multiple file references
CREATE TABLE IF NOT EXISTS diary_attachment_file_reference
(
    attachment_id INTEGER,
    identifier    TEXT NOT NULL,

    FOREIGN KEY (attachment_id) REFERENCES diary_attachment (id),
    FOREIGN KEY (identifier) REFERENCES diary_attachment_file (identifier)
);
-- diary attachment data table
CREATE TABLE IF NOT EXISTS diary_attachment
(
    id          INTEGER PRIMARY KEY,
    title       TEXT NOT NULL,
    description TEXT NOT NULL
);
-- diary attachment settings info table
CREATE TABLE IF NOT EXISTS diary_attachment_info
(
    info_json TEXT NOT NULL PRIMARY KEY
);
-- a mapping table between diary and attachment; a diary can have multiple attachments
CREATE TABLE IF NOT EXISTS diary_attachment_mapping
(
    diary_date             INTEGER,
    referred_attachment_id INTEGER,

    FOREIGN KEY (diary_date) REFERENCES diary ("date"),
    FOREIGN KEY (referred_attachment_id) REFERENCES diary_attachment (id)
);""".split(";\n")
            statements.forEach {
                database.exec(it)
            }
        }

        fun getDateFromDateInt(dateInt: Int): Date {
            val myDate = DiaryTakingActivity.MyDate(dateInt)
            val calendar = Calendar.getInstance()
            calendar.set(myDate.year, myDate.month - 1, myDate.day)
            return calendar.time
        }

        @Throws(IOException::class)
        fun computeIdentifier(f: File): String {
            val `is`: InputStream = FileInputStream(f)
            val md: MessageDigest
            md = try {
                MessageDigest.getInstance("SHA1")
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }
            DigestUtil.updateInputStream(md, `is`)
            val length = f.length()
            val packed = ByteArray(8)
            JNI.Struct.packLong(length, packed, 0, JNI.Struct.MODE_LITTLE_ENDIAN)
            md.update(packed)
            return DigestUtil.bytesToHexString(md.digest())
        }

        private fun computeIdentifier(data: ByteArray): String {
            val length = data.size.toLong()
            val md: MessageDigest = try {
                MessageDigest.getInstance("SHA1")
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }
            val packed = ByteArray(8)
            JNI.Struct.packLong(length, packed, 0, JNI.Struct.MODE_LITTLE_ENDIAN)
            md.update(data)
            md.update(packed)
            return DigestUtil.bytesToHexString(md.digest())
        }

        fun computeIdentifier(s: String): String {
            return computeIdentifier(s.toByteArray(StandardCharsets.UTF_8))
        }
    }

    protected lateinit var internalDatabasePath: String
    protected lateinit var diaryDatabase: SQLite3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!diaryDatabaseRef.isInitialized() || diaryDatabaseRef.isAbandoned()) {
            internalDatabasePath = Common.getInternalDatabaseDir(this, "diary.db").path
            setDatabase(internalDatabasePath)
        }
        diaryDatabaseRef.countRef()
        this.diaryDatabase = diaryDatabaseRef.database
    }

    override fun finish() {
        diaryDatabaseRef.countDownRef()
        super.finish()
    }

    /**
     * Open or change database.
     */
    protected fun setDatabase(path: String) {
        val database = SQLite3.open(path)
        diaryDatabaseRef.set(database)
        initDatabase(database)
        this.diaryDatabase = diaryDatabaseRef.database
    }

    class DiaryDatabaseRef {
        internal lateinit var database: SQLite3
        private var diaryDatabaseRefCount = 0

        fun countRef() {
            ++diaryDatabaseRefCount
        }

        fun countDownRef() {
            if (--diaryDatabaseRefCount == 0) {
                close()
            }
        }

        internal fun close() {
            database.close()
        }

        fun getRefCount(): Int {
            return this.diaryDatabaseRefCount
        }

        /**
         * Return if the database this reference object maintained is released or closed.
         * If returns true, this reference object should be instantiated again.
         */
        fun isAbandoned(): Boolean {
            return this.diaryDatabaseRefCount == 0
        }

        fun isInitialized(): Boolean {
            return this::database.isInitialized
        }

        internal fun set(database: SQLite3) {
            this.database = database
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) onBackPressed()
        return super.onOptionsItemSelected(item)
    }
}