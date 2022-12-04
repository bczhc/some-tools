package pers.zhc.tools.colorpicker

import android.app.Notification
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import pers.zhc.tools.BaseService
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R

class ScreenColorPickerService : BaseService() {
    private var projectionData: Intent? = null
    private val receivers = object {
        lateinit var colorPickerOperation: ScreenColorPickerOperationReceiver
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        startForeground(
            System.currentTimeMillis().hashCode(),
            buildForegroundNotification()
        )

        receivers.colorPickerOperation = ScreenColorPickerOperationReceiver(this).also {
            registerReceiver(it, IntentFilter().apply {
                addAction(ScreenColorPickerOperationReceiver.ACTION_START)
                addAction(ScreenColorPickerOperationReceiver.ACTION_STOP)
            })
        }

        ScreenColorPickerMainActivity.serviceRunning = true

        applicationContext.sendBroadcast(Intent(ScreenColorPickerCheckpointReceiver.ACTION_SERVICE_STARTED))
    }

    override fun onDestroy() {
        applicationContext.unregisterReceiver(receivers.colorPickerOperation)
        ScreenColorPickerMainActivity.serviceRunning = false
    }

    private fun buildForegroundNotification(): Notification {
        val builder = NotificationCompat.Builder(applicationContext, MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL)
        return builder.apply {
            setSmallIcon(R.drawable.ic_db)
            setOngoing(true)
            setContentTitle(getString(R.string.screen_color_picker_running_notification_title))
        }.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent!!
        projectionData = intent.getParcelableExtra(EXTRA_PROJECTION_DATA)
        return START_NOT_STICKY
    }

    fun start(requestId: String): ScreenColorPickerManager {
        val colorPickerManager = ScreenColorPickerManager(this, requestId, projectionData!!)
        colorPickerManager.start()
        return colorPickerManager
    }

    companion object {
        const val EXTRA_PROJECTION_DATA = "projectionData"
    }
}