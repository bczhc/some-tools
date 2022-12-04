package pers.zhc.tools.colorpicker

import android.content.Context
import android.content.Intent
import pers.zhc.tools.BaseBroadcastReceiver
import pers.zhc.tools.utils.androidAssert

class ScreenColorPickerResultReceiver(private val callback: (requestId: String, color: Int) -> Unit): BaseBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent!!
        androidAssert(intent.hasExtra(EXTRA_PICKED_COLOR))
        androidAssert(intent.hasExtra(EXTRA_REQUEST_ID))

        val color = intent.getIntExtra(EXTRA_PICKED_COLOR, 0)
        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)!!
        callback(requestId, color)
    }

    companion object {
        const val ACTION_ON_COLOR_PICKED = "pers.zhc.tools.ACTION_ON_COLOR_PICKED"

        /**
         * string intent extra
         */
        const val EXTRA_REQUEST_ID = StartColorPickerViewReceiver.EXTRA_REQUEST_ID

        /**
         * int intent extra
         */
        const val EXTRA_PICKED_COLOR = "pickedColor"
    }
}