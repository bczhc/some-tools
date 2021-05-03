package pers.zhc.tools.bus

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.utils.Common

/**
 * @author bczhc
 */
class BusArrivalReminderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        Log.d("b", "received: $intent")
        return
        if (intent.action == BaseActivity.BroadcastAction.ACTION_BUS_CANCEL_CLICK) {
            context!!
            Common.doAssertion(intent.hasExtra(EXTRA_NOTIFICATION_ID))
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            context.stopService(Intent(context, BusArrivalReminderService::class.java))
        }
    }

    companion object {
        /**
         * integer extra
         * ID of the notification to cancel
         */
        const val EXTRA_NOTIFICATION_ID = "notificationId"
    }
}