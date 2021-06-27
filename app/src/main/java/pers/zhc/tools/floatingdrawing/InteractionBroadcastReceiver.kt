package pers.zhc.tools.floatingdrawing

import android.content.Context
import android.content.Intent
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.utils.Common

/**
 * @author bczhc
 */
class InteractionBroadcastReceiver(val fb: FloatingDrawingBoardMainActivity) : BaseBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        if (intent.action == ACTION_FB) {
            Common.doAssertion(intent.hasExtra(EXTRA_SCREEN_ORIENTATION))
            val orientation = intent.getIntExtra(EXTRA_SCREEN_ORIENTATION, 0)
            fb.onScreenOrientationChanged(orientation)
        }
    }

    companion object {
        const val ACTION_FB = "pers.zhc.tools.FB"

        /**
         * Intent integer extra
         * is a value of [android.content.res.Configuration.ORIENTATION_LANDSCAPE] or [android.content.res.Configuration.ORIENTATION_PORTRAIT]
         */
        const val EXTRA_SCREEN_ORIENTATION = "screenOrientation"
    }
}