package pers.zhc.tools.bus

import android.content.Context
import android.content.Intent
import android.util.Log
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.utils.Common

/**
 * @author bczhc
 */
class BusArrivalReminderNotificationReceiver : BaseBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        Log.d(TAG, "received")
        if (intent.action == ACTION_BUS_CANCEL_CLICK) {
            context!!
            Common.doAssertion(intent.hasExtra(EXTRA_NOTIFICATION_ID))
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

            val busReminderList = BusArrivalReminderService.busReminderList
            val busReminder = busReminderList[notificationId]
            busReminder.stop()
            busReminderList.remove(notificationId)

            if (busReminderList.size() == 0) {
                // has no bus reminder running, stop the service, to release resources
                context.applicationContext.stopService(Intent(context, BusArrivalReminderService::class.java))
            }
        }
    }

    companion object {
        /**
         * integer extra
         * ID of the notification to cancel
         */
        const val EXTRA_NOTIFICATION_ID = "notificationId"

        const val ACTION_BUS_CANCEL_CLICK = "pers.zhc.tools.BUS_CANCEL_CLICK"
    }
}