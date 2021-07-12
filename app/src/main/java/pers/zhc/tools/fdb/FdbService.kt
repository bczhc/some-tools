package pers.zhc.tools.fdb

import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import pers.zhc.tools.BaseService
import pers.zhc.tools.fdb.FdbBroadcastReceiver.Companion.ACTION_ON_SCREEN_ORIENTATION_CHANGED
import pers.zhc.tools.fdb.FdbBroadcastReceiver.Companion.EXTRA_ORIENTATION

/**
 * @author bczhc
 */
class FdbService: BaseService() {
    private var lastOrientation: Int? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val orientation = newConfig.orientation
        if (lastOrientation == null || lastOrientation!! == orientation) {
            val intent = Intent(ACTION_ON_SCREEN_ORIENTATION_CHANGED)
            intent.putExtra(EXTRA_ORIENTATION, orientation)
            sendBroadcast(intent)
        }
    }
}