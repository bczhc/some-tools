package pers.zhc.tools.floatingdrawing

import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import android.util.Log
import pers.zhc.tools.BaseService

class FloatingDrawingBoardService : BaseService() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: service started")
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val intent = Intent(InteractionBroadcastReceiver.ACTION_FB)
        intent.putExtra(InteractionBroadcastReceiver.EXTRA_SCREEN_ORIENTATION, newConfig.orientation)
        sendBroadcast(intent)
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: service created")
        super.onCreate()
    }
}