package pers.zhc.tools.fdb

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.BaseService
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common

/**
 * @author bczhc
 */
class ScreenColorPickerService : BaseService() {
    private val showerMap = HashMap<Long, ScreenColorPickerShower>()
    private var foregroundId = 0
    private lateinit var stopRequestReceiver: StopRequestReceiver

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: created")
        val id = System.currentTimeMillis().hashCode()
        startForeground(id, buildForegroundNotification())
        foregroundId = id

        stopRequestReceiver = StopRequestReceiver(this)
        applicationContext.registerReceiver(
            stopRequestReceiver,
            IntentFilter(StopRequestReceiver.ACTION_SCREEN_COLOR_PICKER_STOP)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: started")
        intent!!

        Common.doAssertion(intent.hasExtra(EXTRA_FDB_ID))
        val fdbId = intent.getLongExtra(EXTRA_FDB_ID, 0L)

        val projectionData = intent.getParcelableExtra<Intent>(EXTRA_PROJECTION_DATA)!!
        val shower = ScreenColorPickerShower(this, fdbId, projectionData)
        showerMap[fdbId] = shower
        shower.start()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        applicationContext.unregisterReceiver(stopRequestReceiver)
    }

    private fun buildForegroundNotification(): Notification {
        val pi = PendingIntent.getBroadcast(this, System.currentTimeMillis().hashCode(), Intent(), 0)
        val builder = NotificationCompat.Builder(applicationContext, MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL)
        builder.apply {
            setSmallIcon(R.drawable.ic_db)
            setOngoing(true)
            setContentTitle(getString(R.string.screen_color_picker_running_notification_title))
            // just for the touch animation, no operations
            setContentIntent(pi)
        }
        return builder.build()
    }

    class StopRequestReceiver(private val service: ScreenColorPickerService) : BaseBroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            Common.doAssertion(intent.hasExtra(EXTRA_FDB_ID))
            val fdbId = intent.getLongExtra(EXTRA_FDB_ID, 0L)
            if (intent.action == ACTION_SCREEN_COLOR_PICKER_STOP) {
                val showerMap = service.showerMap
                showerMap[fdbId]!!.stop()
                showerMap.remove(fdbId)
                if (showerMap.size == 0) {
                    // there's no more shower running
                    service.stopForeground(true)
                    service.stopSelf()
                }
            }
        }

        companion object {
            const val ACTION_SCREEN_COLOR_PICKER_STOP = "pers.zhc.tools.ACTION_SCREEN_COLOR_PICKER_STOP"

            /**
             * intent long extra
             */
            const val EXTRA_FDB_ID = "fdbId"
        }
    }

    companion object {
        /**
         * intent long extra
         */
        const val EXTRA_FDB_ID = "fdbId"

        /**
         * intent parcelable extra
         */
        const val EXTRA_PROJECTION_DATA = "projectionData"
    }
}