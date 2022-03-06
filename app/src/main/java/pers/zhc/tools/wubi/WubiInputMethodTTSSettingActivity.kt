package pers.zhc.tools.wubi

import android.os.Bundle
import android.widget.Switch
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R

class WubiInputMethodTTSSettingActivity : BaseActivity() {
    companion object {
        @JvmStatic
        var isEnabledTTS: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wubi_input_method_tts_setting_activity)
        val switch = findViewById<Switch>(R.id.tts_switch)
        switch.isChecked = isEnabledTTS
        switch.setOnCheckedChangeListener { _, isChecked ->
            isEnabledTTS = isChecked
        }
    }
}