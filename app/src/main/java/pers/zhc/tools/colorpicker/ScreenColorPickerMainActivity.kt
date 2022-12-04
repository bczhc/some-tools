package pers.zhc.tools.colorpicker

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.ColorPickerMainBinding
import pers.zhc.tools.utils.ToastUtils

class ScreenColorPickerMainActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = ColorPickerMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        bindings.button.setOnClickListener {
            if (!serviceRunning) {
                startActivity(Intent(this, ScreenColorPickerActivity::class.java))
            } else {
                // notify for a color picker window
                val intent = Intent(StartColorPickerViewReceiver.ACTION_START_COLOR_PICKER_VIEW).apply {
                    putExtra(StartColorPickerViewReceiver.EXTRA_REQUEST_ID, "1")
                }
                sendBroadcast(intent)
            }

            val receiver = ScreenColorPickerResultReceiver {requestId, color ->
                ToastUtils.show(this, "$requestId $color")
            }
            registerReceiver(receiver, IntentFilter().apply {
                addAction(ScreenColorPickerResultReceiver.ACTION_ON_COLOR_PICKED)
            })
        }
    }

    companion object {
        var serviceRunning = false
    }
}