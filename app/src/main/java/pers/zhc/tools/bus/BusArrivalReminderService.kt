package pers.zhc.tools.bus

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.ToastUtils
import java.util.*
import kotlin.math.min

/**
 * @author bczhc
 */
class BusArrivalReminderService : Service() {
    private lateinit var receiver: BusArrivalReminderNotificationReceiver
    private lateinit var stationName: String
    private lateinit var direction: BusLineDetailActivity.Direction
    private lateinit var stationId: String
    private lateinit var runPathId: String
    private val TAG = BusArrivalReminderService::class.java.name
    private lateinit var timer: Timer
    private var notificationId = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.i(TAG, "$TAG started")
        receiver = BusArrivalReminderNotificationReceiver()
        val filter = IntentFilter(BaseActivity.BroadcastAction.ACTION_BUS_CANCEL_CLICK)
        registerReceiver(receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationId = System.currentTimeMillis().hashCode()
        intent!!
        runPathId = intent.getStringExtra(EXTRA_RUN_PATH_ID)!!
        stationId = intent.getStringExtra(EXTRA_BUS_STATION_ID)!!
        stationName = intent.getStringExtra(EXTRA_BUS_STATION_NAME)!!
        direction = intent.getSerializableExtra(EXTRA_DIRECTION)!! as BusLineDetailActivity.Direction

        Thread {
            timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    scheduledTask()
                }
            }, 0, 10000 /* 10 s */)
        }.start()
        return START_NOT_STICKY
    }

    private fun scheduledTask() {
        Log.i(TAG, "scheduledTask(...)")
        val busRunList = BusLineDetailActivity.syncFetchBusRunInfo(runPathId, direction)
        val busStationList = BusLineDetailActivity.syncFetchBusStationsInfo(runPathId, direction)
        if (busRunList == null || busStationList == null) {
            ToastUtils.show(this, R.string.bus_fetch_data_failed)
            return
        }

        var myStationIndex = -1

        busStationList.forEachIndexed { index, station ->
            if (station.busStationId == stationId) {
                myStationIndex = index
            }
        }

        Common.doAssertion(myStationIndex != -1)

        class BusRunStation(val index: Int, val arrived: Boolean)

        val busRunStationList = ArrayList<BusRunStation>()

        busRunList.forEach { busRun ->
            busStationList.forEachIndexed { index, busStation ->
                if (busStation.busStationId == busRun.busStationId) {
                    busRunStationList.add(BusRunStation(index, busRun.arrived))
                }
            }
        }

        class NearestBusRunInfo(val busRunStationIndex: Int, val busRunStationName: String)

        val nearestBusRunStationIndex: Int
        val nearestBusRunInfo: NearestBusRunInfo?

        var nearestBusRunStationDiff: Int? = null
        busRunStationList.forEach {
            val d = myStationIndex - it.index
            Log.d(TAG, d.toString())

            if (d >= 0) {
                if (nearestBusRunStationDiff == null) {
                    nearestBusRunStationDiff = d
                }
                nearestBusRunStationDiff = min(nearestBusRunStationDiff!!, d)
            }
        }
        nearestBusRunStationIndex = myStationIndex - nearestBusRunStationDiff!!
        nearestBusRunInfo =
            NearestBusRunInfo(nearestBusRunStationIndex, busStationList[nearestBusRunStationIndex].busStationName)

        if (nearestBusRunStationDiff in 0..2) {
            ToastUtils.show(this, R.string.bus_approaching_toast_msg)
        }

        Common.runOnUiThread(this) {
            notifyNotification(nearestBusRunInfo.busRunStationName, nearestBusRunStationDiff!!)
        }
    }

    private fun notifyNotification(busStationLocation: String, diffToDestStation: Int) {
        val notification = buildNotification(stationName, busStationLocation, diffToDestStation)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(notificationId, notification)
    }

    private fun buildNotification(
        destStationName: String,
        busStationLocation: String,
        subtractionDiffToDestStation: Int
    ): Notification {
        val cancelIntent = Intent(this, BusArrivalReminderNotificationReceiver::class.java)
        cancelIntent.action = BaseActivity.BroadcastAction.ACTION_BUS_CANCEL_CLICK
        val cancelPI = PendingIntent.getBroadcast(this, 0, cancelIntent, 0)

        val notificationContentIntent = Intent()
        notificationContentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val notificationContentPI = PendingIntent.getActivity(this, 0, notificationContentIntent, 0)

        return NotificationCompat.Builder(this, MyApplication.NOTIFICATION_CHANNEL_ID_COMMON).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(getString(R.string.bus_arrival_reminder_notification_title, destStationName))
            setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    getString(
                        R.string.bus_arrival_reminder_notification_content,
                        busStationLocation,
                        subtractionDiffToDestStation
                    )
                )
            )
            setContentIntent(notificationContentPI)
            addAction(R.drawable.ic_launcher_foreground, getString(R.string.cancel_btn), cancelPI)
            setChannelId(MyApplication.NOTIFICATION_CHANNEL_ID_COMMON)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setOngoing(true)
        }.build()
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
    }
}