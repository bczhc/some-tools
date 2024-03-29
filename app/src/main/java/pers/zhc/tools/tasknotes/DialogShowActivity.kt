package pers.zhc.tools.tasknotes

import android.content.Intent
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.ToastUtils

class DialogShowActivity : BaseActivity() {
    private val database = Database.database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Dialog.createRecordEditDialog(this) { record ->
            record ?: run {
                finish()
                return@createRecordEditDialog
            }
            database.insert(record)
            Intent(OnRecordAddedReceiver.ACTION_RECORD_ADDED).let {
                it.putExtra(OnRecordAddedReceiver.EXTRA_CREATION_TIME, record.creationTime)
                sendBroadcast(it)
            }
            ToastUtils.show(this, R.string.adding_succeeded)
            finish()
        }
    }
}