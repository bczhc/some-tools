package pers.zhc.tools.fdb

import android.content.Context
import android.content.Intent
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.utils.Common

/**
 * @author bczhc
 */
class StartScreenColorPickerReceiver(private val onStarted: (fdbId: Long) -> Unit): BaseBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        Common.doAssertion(intent.hasExtra(EXTRA_FDB_ID))
        onStarted(intent.getLongExtra(EXTRA_FDB_ID, 0L))
    }

    companion object {
        /**
         * intent long extra
         */
        const val EXTRA_FDB_ID = "fdbId"

        const val ACTION_SCREEN_COLOR_PICKER_ON_STARTED = "pers.zhc.tools.ACTION_SCREEN_COLOR_PICKER_ON_STARTED"
    }
}