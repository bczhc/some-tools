package pers.zhc.tools.tasknotes

import android.content.Context
import android.content.Intent
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.utils.getLongExtraOrNull

class OnRecordAddedReceiver(
    private val callback: (record: Record) -> Unit
): BaseBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        @Suppress("DEPRECATION")
        val record = intent!!.getSerializableExtra(EXTRA_RECORD) as Record
        callback(record)
    }

    companion object {
        const val ACTION_RECORD_ADDED = "pers.zhc.tools.tasknotes.RECORD_ADDED"

        /**
         * serializable intent extra
         */
        const val EXTRA_RECORD = "record"
    }
}