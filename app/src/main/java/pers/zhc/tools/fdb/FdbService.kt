package pers.zhc.tools.fdb

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import pers.zhc.tools.BaseService
import pers.zhc.tools.fdb.FdbInteractionReceiver.Companion.ACTION_START_FAB
import pers.zhc.tools.fdb.FdbInteractionReceiver.Companion.ACTION_STOP_FAB

/**
 * @author bczhc
 */
class FdbService : BaseService() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: service started")
        return START_NOT_STICKY
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: service created")
        val receiver = FdbInteractionReceiver(this)
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_START_FAB)
            addAction(ACTION_STOP_FAB)
        }
        registerReceiver(receiver, intentFilter)
    }

    fun startFAB() {
        val fdbWindow = FdbWindow(this)
        fdbWindow.startFAB()
    }

    fun stopFAB() {
        val fdbWindow = FdbWindow(this)
        fdbWindow.stopFAB()
    }
}