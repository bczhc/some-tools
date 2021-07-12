package pers.zhc.tools.fdb

import android.content.Intent
import android.os.IBinder
import android.util.Log
import pers.zhc.tools.BaseService

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
    }
}