package pers.zhc.tools.diary

import android.os.Bundle
import android.view.MenuItem
import org.intellij.lang.annotations.Language
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.jni.JNI
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

    /**
     * the shortcut reference to [DiaryDatabase.getDatabaseRef]
     */
    protected lateinit var diaryDatabase: SQLite3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionBar = this.supportActionBar
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        diaryDatabase = DiaryDatabase.getDatabaseRef()
    }

    override fun finish() {
        DiaryDatabase.releaseDatabaseRef()
        super.finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) onBackPressed()
        return super.onOptionsItemSelected(item)
    }
}