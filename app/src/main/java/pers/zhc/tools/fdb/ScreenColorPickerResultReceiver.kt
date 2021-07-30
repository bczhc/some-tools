package pers.zhc.tools.fdb

import android.content.Context
import android.content.Intent
import android.graphics.Color
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.utils.Common

/**
 * @author bczhc
 */
class ScreenColorPickerResultReceiver(private val fdbId: Long, private val callback: ScreenColorPickerResultCallback) :
    BaseBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        Common.doAssertion(intent.hasExtra(EXTRA_FDB_ID))
        Common.doAssertion(intent.hasExtra(EXTRA_PICKED_COLOR))
        val fdbId = intent.getLongExtra(EXTRA_FDB_ID, 0L)
        if (fdbId == this.fdbId) {
            callback(intent.getIntExtra(EXTRA_PICKED_COLOR, Color.TRANSPARENT))
        }
    }

    companion object {
        /**
         * intent long extra
         */
        const val EXTRA_FDB_ID = "fdbID"

        /**
         * intent int extra
         */
        const val EXTRA_PICKED_COLOR = "pickedColor"

        const val ACTION_ON_SCREEN_COLOR_PICKED = "pers.zhc.tools.ACTION_ON_SCREEN_COLOR_PICKED"
    }
}

typealias ScreenColorPickerResultCallback = (color: Int) -> Unit