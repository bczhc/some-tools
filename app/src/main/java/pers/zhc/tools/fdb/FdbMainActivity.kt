package pers.zhc.tools.fdb

import android.os.Bundle
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.android.synthetic.main.fdb_main_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class FdbMainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fdb_main_activity)

        val startButton = start_button!!
        startButton.setOnClickListener {
            val fdbWindow = FdbWindow(this)
            ToastUtils.show(this, fdbWindow.toString())
            fdbWindow.startFDB()
        }
    }
}