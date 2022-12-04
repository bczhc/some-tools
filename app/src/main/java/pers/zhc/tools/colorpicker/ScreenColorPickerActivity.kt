package pers.zhc.tools.colorpicker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.media.CapturePermissionContract
import pers.zhc.tools.utils.ToastUtils

class ScreenColorPickerActivity: BaseActivity() {
    private val permissionRequestLauncher = registerForActivityResult(CapturePermissionContract()) {
        val data = if (it.resultCode == RESULT_OK) {
            it.data!!
        } else {
            ToastUtils.show(this, R.string.capture_permission_denied)
            return@registerForActivityResult
        }

        val serviceIntent = Intent(this, ScreenColorPickerService::class.java).apply {
            putExtra(ScreenColorPickerService.EXTRA_PROJECTION_DATA, data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionRequestLauncher.launch(Unit)
    }
}