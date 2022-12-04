package pers.zhc.tools.colorpicker

import android.content.Context
import android.content.Intent
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.utils.androidAssert

class StartColorPickerViewReceiver(private val service: ScreenColorPickerService) : BaseBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent!!
        androidAssert(intent.hasExtra(EXTRA_REQUEST_ID))

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)!!
        service.start(requestId)
    }

    companion object {
        const val ACTION_START_COLOR_PICKER_VIEW = "pers.zhc.tools.ACTION_START_COLOR_PICKER_VIEW"

        /**
         * string intent extra
         */
        const val EXTRA_REQUEST_ID = "requestId"
    }
}