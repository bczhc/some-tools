package pers.zhc.tools.fdb

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class FdbBroadcastReceiver(private val fdbWindow: FdbWindow) : BaseBroadcastReceiver() {
    private var onScreenOrientationChangedListener: OnScreenOrientationChangedListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: received: $intent")
        intent ?: return

        when (intent.action) {
            ACTION_ON_SCREEN_ORIENTATION_CHANGED -> {
                val orientation = intent.getIntExtra(EXTRA_ORIENTATION, -1)
                Log.d(TAG, "onReceive: new orientation: $orientation")

                onScreenOrientationChangedListener?.invoke(orientation)
            }
            ACTION_FDB_SHOW -> {
                val fdbId = getFdbIdExtra(intent)
                if (fdbWindow.fdbId != fdbId) {
                    return
                }
                fdbWindow.restoreFDB()

                val nm = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(fdbId.hashCode())
            }
            ACTION_ON_CAPTURE_SCREEN_PERMISSION_GRANTED, ACTION_ON_CAPTURE_SCREEN_PERMISSION_DENIED -> {
                val fdbId = getFdbIdExtra(intent)
                if (fdbWindow.fdbId != fdbId) {
                    return
                }

                fdbWindow.startFDB()
            }
        }
    }

    fun getFdbIdExtra(intent: Intent) = run {
        Common.doAssertion(intent.hasExtra(EXTRA_FDB_ID))
        intent.getLongExtra(EXTRA_FDB_ID, 0L)
    }

    companion object {
        const val ACTION_ON_SCREEN_ORIENTATION_CHANGED = "pers.zhc.tools.ACTION_ON_SCREEN_ORIENTATION_CHANGED"

        const val EXTRA_ORIENTATION = "orientation"

        const val ACTION_FDB_SHOW = "pers.zhc.tools.ACTION_FDB_SHOW"
        const val ACTION_ON_CAPTURE_SCREEN_PERMISSION_GRANTED = "pers.zhc.tools.ACTION_ON_CAPTURE_SCREEN_PERMISSION_GRANTED"
        const val ACTION_ON_CAPTURE_SCREEN_PERMISSION_DENIED = "pers.zhc.tools.ACTION_ON_CAPTURE_SCREEN_PERMISSION_DENIED"

        /**
         * Long intent extra
         */
        const val EXTRA_FDB_ID = "FdbId"
    }

    fun setOnScreenOrientationChangedListener(listener: OnScreenOrientationChangedListener?) {
        this.onScreenOrientationChangedListener = listener
    }
}

typealias OnScreenOrientationChangedListener = (orientation: Int) -> Unit