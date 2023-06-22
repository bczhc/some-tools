package pers.zhc.tools.colorpicker

import android.content.Context
import android.content.Intent
import pers.zhc.tools.BaseBroadcastReceiver

class ScreenColorPickerCheckpointReceiver(private val callback: (requestId: String?, action: String) -> Unit) :
    BaseBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent!!
        callback(intent.getStringExtra(EXTRA_REQUEST_ID), intent.action!!)
    }

    companion object {
        const val ACTION_PERMISSION_GRANTED = "pers.zhc.tools.ACTION_SCREEN_COLOR_PICKER_PERMISSION_GRANTED"
        const val ACTION_PERMISSION_DENIED = "pers.zhc.tools.ACTION_SCREEN_COLOR_PICKER_PERMISSION_DEINED"
        const val ACTION_SERVICE_STARTED =
            "pers.zhc.tools.ACTION_SCREEN_COLOR_PICKER_SERVICE_STARTED"

        /**
         * nullable string intent extra
         */
        const val EXTRA_REQUEST_ID = "requestId"
    }
}
