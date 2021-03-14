package pers.zhc.tools.floatingdrawing

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.os.IBinder
import android.view.ViewGroup
import android.view.WindowManager
import pers.zhc.tools.utils.DisplayUtil

/**
 * @author bczhc
 */
class FloatingDrawingBoardService : Service() {
    private lateinit var interactionReceiver: InteractionBroadcastReceiver
    private lateinit var wm: WindowManager
    private lateinit var pv: PaintView
    private lateinit var planeLP: WindowManager.LayoutParams
    private lateinit var paintViewLP: WindowManager.LayoutParams

    fun registerBroadcast() {
        interactionReceiver = this.InteractionBroadcastReceiver()
        val intentFilter = IntentFilter("pers.zhc.tools.floatingdrawing.ACTION_FLOATING_BOARD_ACTIONS")
        registerReceiver(interactionReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        init()
        return START_NOT_STICKY
    }

    private fun getScreenWH(): Point {
        val configuration = this.resources.configuration
        return Point(DisplayUtil.dip2px(this, configuration.screenWidthDp.toFloat()),
            DisplayUtil.dip2px(this, configuration.screenHeightDp.toFloat()))
    }

    private fun init() {
        wm = this.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        pv = PaintView(this)
        // default to match the width and height of `paintViewLP`
        pv.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        pv.drawingStrokeWidth = 10F
        pv.eraserStrokeWidth = 10F
        pv.drawingColor = Color.RED

        planeLP = WindowManager.LayoutParams()
        paintViewLP = WindowManager.LayoutParams()

        planeLP.width = ViewGroup.LayoutParams.WRAP_CONTENT
        planeLP.height = ViewGroup.LayoutParams.WRAP_CONTENT

        val screenWH = getScreenWH()
        paintViewLP.width = screenWH.x
        paintViewLP.height = screenWH.y
    }

    override fun onDestroy() {
        unregisterReceiver(interactionReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun startFloatingWindow() {
        wm.addView(pv, paintViewLP)
    }

    fun stopFloatingWindow() {

    }

    private var lastOrientation: Int? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        val screenWidth = DisplayUtil.dip2px(this, newConfig.screenHeightDp.toFloat())
        val screenHeight = DisplayUtil.dip2px(this, newConfig.screenHeightDp.toFloat())
        val orientation = newConfig.orientation

        if (orientation != lastOrientation) {
            // orientation changed
            lastOrientation = orientation

            paintViewLP.width = screenWidth
            paintViewLP.height = screenHeight
        }
    }

    inner class InteractionBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.getIntExtra("action", 0)) {
                ACTION_START_FLOATING_BOARD -> startFloatingWindow()
                ACTION_STOP_FLOATING_BOARD -> stopFloatingWindow()
                else -> {
                    // no-op
                }
            }
        }
    }

    companion object {
        const val ACTION_START_FLOATING_BOARD = 1
        const val ACTION_STOP_FLOATING_BOARD = 2
    }
}