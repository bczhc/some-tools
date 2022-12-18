package pers.zhc.tools.diary

import android.os.Bundle
import android.view.MenuItem
import pers.zhc.jni.JNI.Struct
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.DigestUtil
import pers.zhc.tools.utils.rc.Ref
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

/**
 * @author bczhc
 */
open class DiaryBaseActivity : BaseActivity() {
    lateinit var diaryDatabaseRef: Ref<DiaryDatabase>

    /**
     * shortcut reference to [DiaryDatabase]
     */
    lateinit var diaryDatabase: DiaryDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionBar = this.supportActionBar
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        diaryDatabaseRef = DiaryDatabase.getDatabaseRef()
        diaryDatabase = diaryDatabaseRef.get()
    }

    override fun finish() {
        diaryDatabaseRef.release()
        super.finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun getDateFromDateInt(dateInt: Int): Date {
            val myDate = MyDate(dateInt)
            val calendar = Calendar.getInstance()
            calendar.set(myDate.year, myDate.month - 1, myDate.day)
            return calendar.time
        }

        fun computeIdentifier(f: File): String {
            return JNI.Diary.computeFileIdentifier(f.path)
        }

        private fun computeIdentifier(data: ByteArray): String {
            val length = data.size.toLong()
            val md = MessageDigest.getInstance("SHA1")
            val packed = ByteArray(8)
            Struct.packLong(length, packed, 0, Struct.MODE_LITTLE_ENDIAN)
            md.update(data)
            md.update(packed)
            return DigestUtil.bytesToHexString(md.digest())
        }

        fun computeIdentifier(s: String): String {
            return computeIdentifier(s.toByteArray(StandardCharsets.UTF_8))
        }
    }
}
