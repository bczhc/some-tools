package pers.zhc.tools.bus

import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import android.util.SparseArray
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.BaseService

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
        busReminderList = SparseArray()
        receiver = BusArrivalReminderNotificationReceiver()
        val filter = IntentFilter(BaseActivity.BroadcastAction.ACTION_BUS_CANCEL_CLICK)
        registerReceiver(receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationId = System.currentTimeMillis().hashCode()

        intent!!
        val runPathId = intent.getStringExtra(EXTRA_RUN_PATH_ID)!!
        val stationId = intent.getStringExtra(EXTRA_BUS_STATION_ID)!!
        val stationName = intent.getStringExtra(EXTRA_BUS_STATION_NAME)!!
        val direction = intent.getSerializableExtra(EXTRA_DIRECTION)!! as BusLineDetailActivity.Direction

        val busReminder = BusReminder(this, runPathId, stationId, stationName, direction, notificationId)
        busReminderList.append(notificationId, busReminder)
        busReminder.start()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "$TAG destroyed")
        unregisterReceiver(receiver)
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