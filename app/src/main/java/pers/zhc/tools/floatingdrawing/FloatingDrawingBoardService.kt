package pers.zhc.tools.floatingdrawing

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.IBinder
import android.view.ViewGroup
import android.view.WindowManager

/**
 * @author bczhc
 */
class FloatingDrawingBoardService : Service() {
    private lateinit var interactionReceiver: InteractionBroadcastReceiver
    private lateinit var wm: WindowManager
    private lateinit var pv: PaintView
    private lateinit var planeLP: WindowManager.LayoutParams
    private lateinit var drawingBoardLP: WindowManager.LayoutParams

    fun registerBroadcast() {
        interactionReceiver = this.InteractionBroadcastReceiver()
        val intentFilter = IntentFilter("pers.zhc.tools.floatingdrawing.ACTION_FLOATING_BOARD_ACTIONS")
        registerReceiver(interactionReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        init()
        return START_NOT_STICKY
    }

    private fun init() {
        wm = this.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        pv = PaintView(this)
        pv.drawingStrokeWidth = 10F
        pv.eraserStrokeWidth = 10F
        pv.drawingColor = Color.RED

        planeLP = WindowManager.LayoutParams()
        drawingBoardLP = WindowManager.LayoutParams()

        planeLP.width = ViewGroup.LayoutParams.WRAP_CONTENT
        planeLP.height = ViewGroup.LayoutParams.WRAP_CONTENT
        drawingBoardLP.width = ViewGroup.LayoutParams.WRAP_CONTENT
        drawingBoardLP.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    override fun onDestroy() {
        unregisterReceiver(interactionReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun startFloatingWindow() {

    }

    fun stopFloatingWindow() {

    }

    inner class InteractionBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val actionInt = intent.getIntExtra("action", 0)
            when (actionInt) {
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