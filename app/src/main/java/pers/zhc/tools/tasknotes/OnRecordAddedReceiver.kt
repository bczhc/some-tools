package pers.zhc.tools.tasknotes

import android.content.Context
import android.content.Intent
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.utils.getLongExtraOrNull

class OnRecordAddedReceiver(
    private val callback: (creationTime: Long) -> Unit
): BaseBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        callback(intent!!.getLongExtraOrNull(EXTRA_CREATION_TIME)!!)
    }

    companion object {
        const val ACTION_RECORD_ADDED = "pers.zhc.tools.tasknotes.RECORD_ADDED"

        /**
         * long intent extra
         * the creation time of the added record
         */
        const val EXTRA_CREATION_TIME = "creationTime"
    }
}