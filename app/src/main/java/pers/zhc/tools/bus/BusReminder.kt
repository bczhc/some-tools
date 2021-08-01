package pers.zhc.tools.bus

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.ToastUtils
import java.util.*
import kotlin.math.min

/**
 * @author bczhc
 */
class BusReminder(
    private val context: Context,
    private val runPathId: String,
    private val stationId: String,
    private val stationName: String,
    private val direction: BusLineDetailActivity.Direction,
    private val notificationId: Int
) {
    private lateinit var notificationManager: NotificationManager
    private lateinit var timer: Timer

    fun start() {
        init()
        timer.schedule(object : TimerTask() {
            override fun run() {
                scheduledTask()
            }
        }, 0, 10000 /* 10 s */)
    }

    fun init() {
        timer = Timer()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun scheduledTask() {
        Log.i(TAG, "scheduledTask(...)")
        val busRunList = BusLineDetailActivity.syncFetchBusRunInfo(runPathId, direction)
        val busStationList = BusLineDetailActivity.syncFetchBusStationsInfo(runPathId, direction)
        val busInfo = BusLineDetailActivity.syncFetchBusInfo(runPathId)
        if (busRunList == null || busStationList == null || busInfo == null) {
            ToastUtils.show(context, R.string.bus_fetch_data_failed)
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
            Common.runOnUiThread(context) {
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
        Common.runOnUiThread(context) {
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
            ToastUtils.show(context, R.string.bus_approaching_toast_msg)
        }
        if (nearestBusRunStationDiff in 0..1) {
            val vibrator = context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE).also {
                    vibrator.vibrate(it)
                }
            } else {
                @Suppress("Deprecation")
                vibrator.vibrate(1000)
            }
        }
    }

    private fun notifyNotification(notification: Notification) {
        notificationManager.notify(notificationId, notification)
    }

    private fun buildNotifyNoBusNotification(busLineName: String, destStationName: String): Notification {
        return buildNotification(
            context.getString(R.string.bus_arrival_reminder_notification_title, busLineName, destStationName),
            context.getString(R.string.bus_no_nearest_bus)
        )
    }

    private fun buildHaveBusNotification(
        busLineName: String,
        destStationName: String,
        busStationLocation: String,
        diffToDestStation: Int
    ): Notification {
        return buildNotification(
            context.getString(R.string.bus_arrival_reminder_notification_title, busLineName, destStationName),
            context.getString(
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
        val cancelIntent = Intent(BusArrivalReminderNotificationReceiver.ACTION_BUS_CANCEL_CLICK)
        cancelIntent.putExtra(BusArrivalReminderNotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        val cancelPI = PendingIntent.getBroadcast(context, notificationId, cancelIntent, 0)!!

        val notificationContentIntent = Intent(context, BusLineDetailActivity::class.java)
        notificationContentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        notificationContentIntent.putExtra(BusLineDetailActivity.EXTRA_RUN_PATH_ID, runPathId)
        notificationContentIntent.putExtra(BusLineDetailActivity.EXTRA_DIRECTION, direction)
        val notificationContentPI = PendingIntent.getActivity(context, notificationId, notificationContentIntent, 0)!!

        return NotificationCompat.Builder(context, MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(contentTitle)
            setStyle(
                NotificationCompat.BigTextStyle().bigText(contentText)
            )
            setContentIntent(notificationContentPI)
            addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.cancel_btn), cancelPI)
            setChannelId(MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setOngoing(true)
        }.build()
    }

    fun stop() {
        timer.cancel()
        notificationManager.cancel(notificationId)
    }

    companion object {
        private val TAG: String = BusReminder::class.java.name
    }
}