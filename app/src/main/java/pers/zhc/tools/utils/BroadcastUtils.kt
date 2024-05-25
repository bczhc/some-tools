package pers.zhc.tools.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat

fun Context.registerReceiverCompat(
    receiver: BroadcastReceiver,
    filter: IntentFilter,
    flags: Int? = null,
    exported: Boolean = false
) {
    ContextCompat.registerReceiver(this, receiver, filter, run {
        val exportFlag = if (exported) {
            ContextCompat.RECEIVER_EXPORTED
        } else {
            ContextCompat.RECEIVER_NOT_EXPORTED
        }
        (flags ?: 0) or exportFlag
    })
}
