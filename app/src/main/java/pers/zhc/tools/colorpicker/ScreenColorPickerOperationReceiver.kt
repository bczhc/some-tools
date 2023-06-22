package pers.zhc.tools.colorpicker

import android.content.Context
import android.content.Intent
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.utils.androidAssert

class ScreenColorPickerOperationReceiver(private val service: ScreenColorPickerService) : BaseBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent!!
        androidAssert(intent.hasExtra(EXTRA_REQUEST_ID))
        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)!!

        when (intent.action) {
            ACTION_START -> {
                service.start(requestId)
            }

            ACTION_STOP -> {
                service.stop(requestId)
            }

            else -> {}
        }
    }

    companion object {
        const val ACTION_START = "pers.zhc.tools.ACTION_START_COLOR_PICKER_VIEW"
        const val ACTION_STOP = "pers.zhc.tools.ACTION_STOP_COLOR_PICKER_VIEW"

        /**
         * string intent extra
         */
        const val EXTRA_REQUEST_ID = "requestId"
    }
}
