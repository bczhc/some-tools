package pers.zhc.tools.fdb

import android.os.Build
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.utils.ToastUtils

class StartingFdbActivity : BaseActivity() {

    private val overlaySetting = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        registerForActivityResult(OverlaySettingContract()) {
            finish()
        }
    } else {
        null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        if (!FdbMainActivity.checkDrawOverlayPermission()) {
            overlaySetting?.launch(null)
        }

        val fdb = FdbMainActivity.createFdbWindow(this).also {
            it.startFDB()
        }
        ToastUtils.show(this, fdb.toString())
        finish()
    }
}
