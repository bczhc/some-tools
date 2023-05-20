package pers.zhc.tools.colorpicker

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.os.Build
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import pers.zhc.tools.floatingdrawing.FloatingViewOnTouchListener
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.DisplayUtil
import pers.zhc.tools.utils.ProjectionScreenshotReader

/**
 * @author bczhc
 */
class ScreenColorPickerManager(
    private val context: Context,
    private val requestId: String,
    private val mediaProjection: MediaProjection
) {
    private lateinit var screenshotReader: ProjectionScreenshotReader
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
            @Suppress("DEPRECATION")
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

        val screenSize = DisplayUtil.getScreenSize(context)
        screenColorPickerView.measure(0, 0)
        screenColorPickerViewDimension.width = 0
        screenColorPickerViewDimension.height = 0
        screenColorPickerViewPositionUpdater.updateParentDimension(screenSize.x, screenSize.y)

        screenColorPickerView.setOnScreenSizeChangedListener { _, _ ->
            val newSize = DisplayUtil.getScreenSize(context)
            screenColorPickerViewPositionUpdater.updateParentDimension(newSize.x, newSize.y)
        }

        screenshotReader = ProjectionScreenshotReader(context, mediaProjection)

        var screenshotDone = true
        @Suppress("ClickableViewAccessibility")
        screenColorPickerView.setOnTouchListener { v, event ->
            screenColorPickerViewPositionUpdater.onTouch(v, event)

            if (!screenshotDone) {
                return@setOnTouchListener false
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    screenshotDone = false
                    screenColorPickerView.setIsTransparent(true)

                    Thread {
                        // manually delay, workaround for that the screenshot contains the colorPickerView itself
                        Thread.sleep(100)
                        screenshotReader.requestScreenshot { bitmap ->
                            Common.runOnUiThread(context) {
                                screenColorPickerView.getBitmap()?.recycle()
                                screenColorPickerView.setBitmap(bitmap)
                                screenColorPickerView.setIsTransparent(false)
                                screenshotDone = true
                            }
                        }
                    }.start()
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
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
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
        val intent = Intent(ScreenColorPickerResultReceiver.ACTION_ON_COLOR_PICKED).apply {
            putExtra(ScreenColorPickerResultReceiver.EXTRA_REQUEST_ID, requestId)
            putExtra(ScreenColorPickerResultReceiver.EXTRA_PICKED_COLOR, color)
        }
        context.applicationContext.sendBroadcast(intent)
    }

    fun stop() {
        wm.removeView(screenColorPickerView)
        screenshotReader.close()
    }
}
