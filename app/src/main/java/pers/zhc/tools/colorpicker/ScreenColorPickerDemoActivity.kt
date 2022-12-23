package pers.zhc.tools.colorpicker

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.ColorPickerMainBinding
import pers.zhc.tools.utils.ToastUtils

class ScreenColorPickerDemoActivity : BaseActivity() {
    private lateinit var resultReceiver: ScreenColorPickerResultReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = ColorPickerMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        bindings.button.setOnClickListener {
            if (!serviceRunning) {
                startActivity(Intent(this, ScreenColorPickerActivity::class.java))
            } else {
                // notify for a color picker window
                val intent = Intent(ScreenColorPickerOperationReceiver.ACTION_START).apply {
                    putExtra(ScreenColorPickerOperationReceiver.EXTRA_REQUEST_ID, "1")
                }
                sendBroadcast(intent)
            }

            resultReceiver = ScreenColorPickerResultReceiver { requestId, color ->
                ToastUtils.show(this, "$requestId $color")
            }.also {
                registerReceiver(it, IntentFilter().apply {
                    addAction(ScreenColorPickerResultReceiver.ACTION_ON_COLOR_PICKED)
                })
            }
        }
    }

    override fun finish() {
        unregisterReceiver(resultReceiver)
        super.finish()
    }

    companion object {
        var serviceRunning = false
    }
}