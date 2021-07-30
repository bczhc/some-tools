package pers.zhc.tools.fdb

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.BaseService
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.floatingdrawing.FloatingViewOnTouchListener
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.DisplayUtil
import pers.zhc.tools.utils.MediaUtils

/**
 * @author bczhc
 */
class ScreenColorPickerService : BaseService() {
    private lateinit var projectionData: Intent
    private var fdbId = 0L

    private lateinit var wm: WindowManager

    private lateinit var screenColorPickerView: ScreenColorPickerView
    private val screenColorPickerViewLP = WindowManager.LayoutParams()

    private val screenColorPickerViewDimension = FloatingViewOnTouchListener.ViewDimension()
    private lateinit var screenColorPickerViewPositionUpdater: FloatingViewOnTouchListener

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent!!

        Common.doAssertion(intent.hasExtra(EXTRA_FDB_ID))
        fdbId = intent.getLongExtra(EXTRA_FDB_ID, 0L)
        projectionData = intent.getParcelableExtra(EXTRA_PROJECTION_DATA)!!

        startForeground(fdbId.hashCode(), buildForegroundNotification())
        startScreenColorPicker()

        val stopReceiver = StopReceiver(wm, screenColorPickerView)
        val filter = IntentFilter(StopReceiver.ACTION_SCREEN_COLOR_PICKER_STOP)
        applicationContext.registerReceiver(stopReceiver, filter)
        return START_NOT_STICKY
    }

    private fun buildForegroundNotification(): Notification {

        val intent = Intent(StopReceiver.ACTION_SCREEN_COLOR_PICKER_STOP)
        val pi = PendingIntent.getBroadcast(applicationContext, fdbId.hashCode(), intent, 0)

        val builder = NotificationCompat.Builder(applicationContext, MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL)
        builder.apply {
            setSmallIcon(R.drawable.ic_db)
            setOngoing(true)
            setContentTitle(getString(R.string.screen_color_picker_running_notification_title))
            setContentText(getString(R.string.press_to_stop))
            setContentIntent(pi)
        }
        return builder.build()
    }

    private fun startScreenColorPicker() {
        wm = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        screenColorPickerView = ScreenColorPickerView(this)
        screenColorPickerViewPositionUpdater = FloatingViewOnTouchListener(
            screenColorPickerViewLP,
            wm,
            screenColorPickerView,
            0,
            0,
            screenColorPickerViewDimension
        )

        val floatingWindowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("deprecation")
            (WindowManager.LayoutParams.TYPE_SYSTEM_ERROR)
        }
        screenColorPickerViewLP.apply {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            type = floatingWindowType
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.RGBA_8888
        }

        val metrics = DisplayUtil.getMetrics(this)
        screenColorPickerView.measure(0, 0)
        screenColorPickerViewDimension.width = screenColorPickerView.measuredWidth
        screenColorPickerViewDimension.height = screenColorPickerView.measuredHeight
        screenColorPickerViewPositionUpdater.updateParentDimension(metrics.widthPixels, metrics.heightPixels)

        var screenshotDone = true
        @Suppress("ClickableViewAccessibility")
        screenColorPickerView.setOnTouchListener { v, event ->
            screenColorPickerViewPositionUpdater.onTouch(v, event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!screenshotDone) {
                        return@setOnTouchListener true
                    }
                    screenshotDone = false
                    screenColorPickerView.setIsTransparent(true)
                    MediaUtils.asyncTakeScreenshot(this, projectionData, null) { image ->
                        val bitmap = MediaUtils.imageToBitmap(image)
                        image.close()
                        screenColorPickerView.getBitmap()?.recycle()
                        screenColorPickerView.setBitmap(bitmap)
                        screenColorPickerView.setIsTransparent(false)
                        screenshotDone = true
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val rawX = event.rawX
                    val rawY = event.rawY
                    val x = event.x
                    val y = event.y
                    val pointX = rawX - x + screenColorPickerView.measuredWidth.toFloat() / 2F
                    val pointY = rawY - y + screenColorPickerView.measuredHeight.toFloat() / 2F
                    screenColorPickerView.updatePosition(pointX, pointY)
                    val color = screenColorPickerView.getColor()
                    color?.let {
                        screenColorPickerView.setColor(it)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    screenColorPickerView.getColor()?.let { onColorPicked(it) }
                }
                else -> {
                }
            }
            return@setOnTouchListener true
        }

        wm.addView(screenColorPickerView, screenColorPickerViewLP)
    }

    private fun onColorPicked(color: Int) {
        val intent = Intent(ScreenColorPickerResultReceiver.ACTION_ON_SCREEN_COLOR_PICKED)
        intent.putExtra(ScreenColorPickerResultReceiver.EXTRA_PICKED_COLOR, color)
        intent.putExtra(ScreenColorPickerResultReceiver.EXTRA_FDB_ID, fdbId)
        sendBroadcast(intent)
    }

    class StopReceiver(private val wm: WindowManager, private val view: View): BaseBroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            if (intent.action == ACTION_SCREEN_COLOR_PICKER_STOP) {
                wm.removeView(view)
            }
        }

        companion object {
            const val ACTION_SCREEN_COLOR_PICKER_STOP = "pers.zhc.tools.ACTION_SCREEN_COLOR_PICKER_STOP"
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