package pers.zhc.tools.fdb

import android.os.Bundle
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.android.synthetic.main.fdb_main_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R

/**
 * @author bczhc
 */
class FdbMainActivity : BaseActivity() {
    lateinit var fdbSwitch: SwitchMaterial
    private lateinit var fdbWindow: FdbWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fdb_main_activity)

        fdbWindow = FdbWindow(this)

        fdbSwitch = fdb_switch!!

        fdbSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startFdb()
            } else {
                stopFdb()
            }
        }
    }

    private fun startFdb() {
        fdbWindow.startFAB()
    }

    private fun stopFdb() {
        fdbWindow.stopFAB()
    }
}