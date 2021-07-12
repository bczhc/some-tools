package pers.zhc.tools.fdb

import android.content.Context
import android.content.Intent
import android.util.Log
import pers.zhc.tools.BaseBroadcastReceiver

/**
 * @author bczhc
 */
class FdbBroadcastReceiver : BaseBroadcastReceiver() {
    private var onScreenOrientationChangedListener: OnScreenOrientationChangedListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return

        if (intent.action == ACTION_ON_SCREEN_ORIENTATION_CHANGED) {
            val orientation = intent.getIntExtra(EXTRA_ORIENTATION, -1)
            Log.d(TAG, "onReceive: new orientation: $orientation")

            onScreenOrientationChangedListener?.invoke(orientation)
        }
    }

    companion object {
        const val ACTION_ON_SCREEN_ORIENTATION_CHANGED = "pers.zhc.tools.ACTION_ON_SCREEN_ORIENTATION_CHANGED"

        const val EXTRA_ORIENTATION = "orientation"
    }

    fun setOnScreenOrientationChangedListener(listener: OnScreenOrientationChangedListener?) {
        this.onScreenOrientationChangedListener = listener
    }
}

typealias OnScreenOrientationChangedListener = (orientation: Int) -> Unit