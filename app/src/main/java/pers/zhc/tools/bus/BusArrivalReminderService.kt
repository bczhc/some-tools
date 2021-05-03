package pers.zhc.tools.bus

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
    private lateinit var notificationContentPI: PendingIntent
    private lateinit var cancelPI: PendingIntent
    private lateinit var notificationManager: NotificationManager
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
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationId = System.currentTimeMillis().hashCode()

        intent!!
        runPathId = intent.getStringExtra(EXTRA_RUN_PATH_ID)!!
        stationId = intent.getStringExtra(EXTRA_BUS_STATION_ID)!!
        stationName = intent.getStringExtra(EXTRA_BUS_STATION_NAME)!!
        direction = intent.getSerializableExtra(EXTRA_DIRECTION)!! as BusLineDetailActivity.Direction

        val cancelIntent = Intent(BaseActivity.BroadcastAction.ACTION_BUS_CANCEL_CLICK)
        cancelIntent.putExtra(BusArrivalReminderNotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        cancelPI = PendingIntent.getBroadcast(this, notificationId, cancelIntent, 0)!!

        val notificationContentIntent = Intent(this, BusLineDetailActivity::class.java)
        notificationContentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        notificationContentIntent.putExtra(BusLineDetailActivity.EXTRA_RUN_PATH_ID, runPathId)
        notificationContentPI = PendingIntent.getActivity(this, 0, notificationContentIntent, 0)!!

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
        val busInfo = BusLineDetailActivity.syncFetchBusInfo(runPathId)
        if (busRunList == null || busStationList == null || busInfo == null) {
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

        class BusRunStation(val index: Int, @Suppress("unused") val arrived: Boolean)

        val busRunStationList = ArrayList<BusRunStation>()

        busRunList.forEach { busRun ->
            busStationList.forEachIndexed { index, busStation ->
                if (busStation.busStationId == busRun.busStationId) {
                    busRunStationList.add(BusRunStation(index, busRun.arrived))
                }
            }
        }

        val nearestBusRunStationIndex: Int
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
        if (nearestBusRunStationDiff == null) {
            // there's no nearest bus
            Common.runOnUiThread(this) {
                notifyNotification(
                    buildNotifyNoBusNotification(
                        busInfo.busLineName,
                        stationName
                    )
                )
            }
            return
        }

        nearestBusRunStationIndex = myStationIndex - nearestBusRunStationDiff!!
        Common.runOnUiThread(this) {
            notifyNotification(
                buildHaveBusNotification(
                    busInfo.busLineName,
                    stationName,
                    busStationList[nearestBusRunStationIndex].busStationName,
                    nearestBusRunStationDiff!!
                )
            )
        }

        if (nearestBusRunStationDiff in 0..2) {
            ToastUtils.show(this, R.string.bus_approaching_toast_msg)
        }
    }

    private fun notifyNotification(notification: Notification) {
        notificationManager.notify(notificationId, notification)
    }

    private fun buildNotifyNoBusNotification(busLineName: String, destStationName: String): Notification {
        return buildNotification(
            getString(R.string.bus_arrival_reminder_notification_title, busLineName, destStationName),
            getString(R.string.bus_no_nearest_bus)
        )
    }

    private fun buildHaveBusNotification(
        busLineName: String,
        destStationName: String,
        busStationLocation: String,
        diffToDestStation: Int
    ): Notification {
        return buildNotification(
            getString(R.string.bus_arrival_reminder_notification_title, busLineName, destStationName),
            getString(
                R.string.bus_arrival_reminder_notification_content,
                busStationLocation,
                diffToDestStation
            )
        )
    }

    private fun buildNotification(
        contentTitle: String,
        contentText: String
    ): Notification {
        return NotificationCompat.Builder(this, MyApplication.NOTIFICATION_CHANNEL_ID_COMMON).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(contentTitle)
            setStyle(
                NotificationCompat.BigTextStyle().bigText(contentText)
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