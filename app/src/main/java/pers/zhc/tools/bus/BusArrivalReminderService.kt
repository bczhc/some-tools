package pers.zhc.tools.bus

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.SparseArray
import androidx.core.app.NotificationCompat
import pers.zhc.tools.BaseService
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.utils.getSerializableExtra
import pers.zhc.tools.utils.registerReceiverCompat

/**
 * @author bczhc
 */
class BusArrivalReminderService : BaseService() {
    private lateinit var receiver: BusArrivalReminderNotificationReceiver

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.i(TAG, "$TAG started")
        startForeground(System.currentTimeMillis().hashCode(), createForegroundNotification())

        busReminderList = SparseArray()
        receiver = BusArrivalReminderNotificationReceiver()
        val filter = IntentFilter(BusArrivalReminderNotificationReceiver.ACTION_BUS_CANCEL_CLICK)
        registerReceiverCompat(receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationId = System.currentTimeMillis().hashCode()

        intent!!
        val runPathId = intent.getStringExtra(EXTRA_RUN_PATH_ID)!!
        val stationId = intent.getStringExtra(EXTRA_BUS_STATION_ID)!!
        val stationName = intent.getStringExtra(EXTRA_BUS_STATION_NAME)!!
        val direction = intent.getSerializableExtra(EXTRA_DIRECTION, BusLineDetailActivity.Direction::class)!!

        val busReminder = BusReminder(this, runPathId, stationId, stationName, direction, notificationId)
        busReminderList.append(notificationId, busReminder)
        busReminder.start()

        return START_STICKY
    }

    private fun createForegroundNotification(): Notification {
        val pi = PendingIntent.getBroadcast(
            this, System.currentTimeMillis().hashCode(), Intent(), if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        )

        val builder = NotificationCompat.Builder(this, MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL)
        builder.apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(getString(R.string.bus_foreground_notification_title))
            // no operations
            setContentIntent(pi)
        }

        return builder.build()
    }

    override fun onDestroy() {
        Log.i(TAG, "$TAG destroyed")
        unregisterReceiver(receiver)
        stopForeground(true)
    }

    companion object {
        /**
         * string extra
         */
        const val EXTRA_RUN_PATH_ID = "busRunId"

        /**
         * string extra
         * the destination station id
         */
        const val EXTRA_BUS_STATION_ID = "busStationId"

        /**
         * serializable extra
         * the bus direction
         */
        const val EXTRA_DIRECTION = "direction"

        /**
         * string extra
         * the destination station name
         */
        const val EXTRA_BUS_STATION_NAME = "busStationName"

        lateinit var busReminderList: SparseArray<BusReminder>
    }
}
