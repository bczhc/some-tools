package pers.zhc.tools.fdb

import android.content.Context
import android.content.Intent
import android.util.Log
import pers.zhc.tools.BaseBroadcastReceiver
import kotlin.math.log

/**
 * @author bczhc
 */
class FdbInteractionReceiver(private val service: FdbService) : BaseBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

    }

    companion object {
        const val ACTION_START_FAB = "pers.zhc.tools.startFAB"

        const val ACTION_STOP_FAB = "pers.zhc.tools.stopFAB"
    }
}