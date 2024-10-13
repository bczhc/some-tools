package pers.zhc.tools.diary

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import pers.zhc.jni.JNI.Struct
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.DiaryRecordStatDialogBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.*
import pers.zhc.tools.utils.rc.Ref
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

/**
 * @author bczhc
 */
open class DiaryBaseActivity : BaseActivity() {
    var diaryDatabaseRef: Ref<DiaryDatabase>? = null

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

        if (!validateDatabase(DiaryDatabase.internalDatabasePath, LocalConfig.readPassword())) {
            showPasswordPromptDialog(this, DiaryDatabase.internalDatabasePath, onSuccess = {
                val config = LocalConfig.read().apply {
                    password = it
                }
                LocalConfig.write(config)
            })
        } else {
            diaryDatabaseRef = DiaryDatabase.getDatabaseRef()
            diaryDatabase = diaryDatabaseRef!!.get()
            onDatabaseValidated()
        }
    }

    open fun onDatabaseValidated() {}

    override fun finish() {
        diaryDatabaseRef?.release()
        super.finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }

    companion object {
        fun getDateFromDateInt(dateInt: Int): Date {
            val myDate = IntDate(dateInt)
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

        private fun validateDatabase(database: File, password: String): Boolean {
            val db = SQLite3.open(database.path)
            db.key(password)
            val isADatabase = !db.checkIfCorrupt()
            db.close()
            return isADatabase
        }

        fun showPasswordPromptDialog(
            context: Context,
            db: File,
            onCancel: () -> Unit = {},
            onSuccess: (password: String) -> Unit
        ) {
            val dialog = passwordPromptDialog(context, { p, r ->
                thread {
                    r(validateDatabase(db, p))
                }
            }, onCancel = onCancel, onSuccess = onSuccess)
            dialog.show()
        }

        fun createDiaryRecordStatDialog(context: Context, database: DiaryDatabase, dateInt: Int): Dialog {
            return createDiaryRecordStatDialog(context, database.getCharsCount(dateInt))
        }

        fun createDiaryRecordStatDialog(context: Context, count: Int): Dialog {
            return Dialog(context).apply {
                setTitle(R.string.diary_statistics_dialog_title)
                val bindings = DiaryRecordStatDialogBinding.inflate(LayoutInflater.from(context)).apply {
                    statContentTv.text = context.getString(
                        R.string.diary_record_statistics_dialog_content,
                        count
                    )
                }
                setContentView(bindings.root)
                DialogUtils.setDialogAttr(this, width = MATCH_PARENT)
            }
        }
    }
}
