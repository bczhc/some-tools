package pers.zhc.tools.bus

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * @author bczhc
 */
class BusArrivalReminderService: Service() {
    private val TAG = BusArrivalReminderService::class.java.name

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.i(TAG, "$TAG started")
    }

    override fun onDestroy() {
        Log.i(TAG, "$TAG destroyed")
    }
}