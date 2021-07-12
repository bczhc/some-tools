package pers.zhc.tools.fdb

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.R
import pers.zhc.tools.fdb.HiddenFdbHolder.fdbWindowMap
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class FdbNotificationReceiver: BaseBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: received, intent: $intent")

        intent ?: return
        if (intent.action == ACTION_FDB_SHOW) {
            val id = intent.getLongExtra(EXTRA_FDB_ID, 0)
            val fdbWindow = fdbWindowMap[id]
            if (fdbWindow == null) {
                ToastUtils.show(context, R.string.fdb_fdb_window_lost_toast)
                return
            }
            fdbWindowMap.remove(id)
            fdbWindow.restoreFDB()

            val nm = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(id.hashCode())
        }
    }

    companion object {
        const val ACTION_FDB_SHOW = "pers.zhc.tools.ACTION_FDB_SHOW"

        /**
         * Long intent extra
         */
        const val EXTRA_FDB_ID = "FdbId"
    }
}