package pers.zhc.tools.fdb

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import pers.zhc.tools.floatingdrawing.FloatingViewOnTouchListener
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.DisplayUtil
import pers.zhc.tools.utils.MediaUtils

/**
 * @author bczhc
 */
class ScreenColorPickerShow(
    private val context: Context,
    private val fdbId: Long,
    private val projectionData: Intent
) {
    private lateinit var wm: WindowManager
    private lateinit var screenColorPickerView: ScreenColorPickerView
    private val screenColorPickerViewLP = WindowManager.LayoutParams()

    private val screenColorPickerViewDimension = FloatingViewOnTouchListener.ViewDimension()
    private lateinit var screenColorPickerViewPositionUpdater: FloatingViewOnTouchListener

    fun start() {
        wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        screenColorPickerView = ScreenColorPickerView(context)
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
            flags = FLAG_NOT_FOCUSABLE
                .xor(FLAG_LAYOUT_NO_LIMITS)
            format = PixelFormat.RGBA_8888
        }

        val metrics = DisplayUtil.getMetrics(context)
        screenColorPickerView.measure(0, 0)
        screenColorPickerViewDimension.width = 0
        screenColorPickerViewDimension.height = 0
        screenColorPickerViewPositionUpdater.updateParentDimension(metrics.widthPixels, metrics.heightPixels)

        screenColorPickerView.setOnScreenSizeChangedListener { width, height ->
            val newMetrics = DisplayUtil.getMetrics(context)
            screenColorPickerViewPositionUpdater.updateParentDimension(newMetrics.widthPixels, newMetrics.heightPixels)
        }

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
                    MediaUtils.asyncTakeScreenshot(context, projectionData) { bitmap ->
                        Common.runOnUiThread(context) {
                            screenColorPickerView.getBitmap()?.recycle()
                            screenColorPickerView.setBitmap(bitmap)
                            screenColorPickerView.setIsTransparent(false)
                            screenshotDone = true
                        }
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
                    try {
                        val color = screenColorPickerView.getColor()
                        color?.let {
                            screenColorPickerView.setColor(it)
                        }
                    } catch (e: Exception) {
                        // at least it prevents the accident coordinates out of index exception
                        Common.showException(e, context)
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
        context.applicationContext.sendBroadcast(intent)
    }

    fun stop() {
        wm.removeView(screenColorPickerView)
    }
}