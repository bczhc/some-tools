package pers.zhc.tools.diary

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.jni.JNI.Struct
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.DiaryEnterPasswordDialogBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.DigestUtil
import pers.zhc.tools.utils.IntDate
import pers.zhc.tools.utils.rc.Ref
import pers.zhc.tools.utils.setNegativeAction
import pers.zhc.tools.utils.setPositiveAction
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

        if (!validateDatabase()) {
            showPasswordDialog()
        } else {
            diaryDatabaseRef = DiaryDatabase.getDatabaseRef()
            diaryDatabase = diaryDatabaseRef!!.get()
            onDatabaseValidated()
        }
    }

    open fun onDatabaseValidated() {}

    private fun showPasswordDialog() {
        // validate password of the database
        val dialogBindings = DiaryEnterPasswordDialogBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setNegativeAction()
            .setOnCancelListener {
                finish()
            }
            .setPositiveAction()
            .setView(dialogBindings.root)
            .setTitle(R.string.enter_password_dialog_title)
            .create()
        dialog.setOnShowListener {
            (it as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    dialogBindings.progressBar.visibility = View.VISIBLE
                    val config = LocalConfig.read()
                    config.password = dialogBindings.passwordEt.text.toString()
                }
        }
        dialog.show()
    }

    private fun getSavedPassword(): String? {
        val config = LocalConfig.read()
        return config.password
    }

    private fun validateDatabase(): Boolean {
        val password = getSavedPassword() ?: DiaryDatabase.DEFAULT_PASSPHRASE
        val db = SQLite3.open(DiaryDatabase.internalDatabasePath.path)
//        db.key(password)
        db.exec("PRAGMA key = 'a'")
        val isADatabase = !db.checkIfCorrupt()
        db.close()
        return isADatabase
    }

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
    }
}
