package pers.zhc.tools.tasknotes

import android.content.Intent
import android.os.Bundle
import pers.zhc.tools.BaseActivity

class DialogShowActivity : BaseActivity() {
    private val database = Database.database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Dialog.createRecordEditDialog(this) { record ->
            record ?: run {
                finish()
                return@createRecordEditDialog
            }
            Intent(OnRecordAddedReceiver.ACTION_RECORD_ADDED).let {
                it.putExtra(OnRecordAddedReceiver.EXTRA_RECORD, record)
                sendBroadcast(it)
            }
            finish()
        }
    }
}