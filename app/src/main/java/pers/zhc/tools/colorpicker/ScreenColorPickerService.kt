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

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        startForeground(
            System.currentTimeMillis().hashCode(),
            buildForegroundNotification()
        )

        val receiver = StartColorPickerViewReceiver(this)
        registerReceiver(receiver, IntentFilter().apply {
            addAction(StartColorPickerViewReceiver.ACTION_START_COLOR_PICKER_VIEW)
        })

        ScreenColorPickerMainActivity.serviceRunning = true

        applicationContext.sendBroadcast(Intent(ScreenColorPickerCheckpointReceiver.ACTION_SERVICE_STARTED))
    }

    override fun onDestroy() {
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

    fun start(requestId: String) {
        val colorPickerManager = ScreenColorPickerManager(this, requestId, projectionData!!)
        colorPickerManager.start()
    }

    companion object {
        const val EXTRA_PROJECTION_DATA = "projectionData"
    }
}