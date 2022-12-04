package pers.zhc.tools.colorpicker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.media.CapturePermissionContract
import pers.zhc.tools.utils.ToastUtils

/**
 * This activity requests the screen capture permission
 * and then starts the color picker service
 *
 * after the service is started, send [ScreenColorPickerOperationReceiver.ACTION_START] broadcast
 * to start a new color picker view. Also [ScreenColorPickerOperationReceiver.ACTION_STOP] for
 * stopping the color picker view.
 *
 * Each request needs a [ScreenColorPickerOperationReceiver.EXTRA_REQUEST_ID], and this allows multiple
 * color picker view
 */
class ScreenColorPickerActivity: BaseActivity() {
    private val permissionRequestLauncher = registerForActivityResult(CapturePermissionContract()) {
        when (it.resultCode) {
            RESULT_OK -> {
                applicationContext.sendBroadcast(Intent(ScreenColorPickerCheckpointReceiver.ACTION_PERMISSION_GRANTED))
                onPermissionGranted(it.data!!)
                finish()
            }
            RESULT_CANCELED -> {
                ToastUtils.show(this, R.string.capture_permission_denied)
                applicationContext.sendBroadcast(Intent(ScreenColorPickerCheckpointReceiver.ACTION_PERMISSION_DENIED))
                finish()
            }
        }
    }

    private fun onPermissionGranted(projectionData: Intent) {
        val serviceIntent = Intent(this, ScreenColorPickerService::class.java).apply {
            putExtra(ScreenColorPickerService.EXTRA_PROJECTION_DATA, projectionData)
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