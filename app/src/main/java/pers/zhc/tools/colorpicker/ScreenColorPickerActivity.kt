package pers.zhc.tools.colorpicker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.colorpicker.ScreenColorPickerCheckpointReceiver.Companion.EXTRA_REQUEST_ID
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
class ScreenColorPickerActivity : BaseActivity() {
    private var requestId: String? = null

    private val permissionRequestLauncher = registerForActivityResult(CapturePermissionContract()) {
        when (it.resultCode) {
            RESULT_OK -> {
                val intent = Intent(ScreenColorPickerCheckpointReceiver.ACTION_PERMISSION_GRANTED).apply {
                    putExtra(EXTRA_REQUEST_ID, requestId)
                }
                applicationContext.sendBroadcast(intent)
                onPermissionGranted(it.data!!)
                finish()
            }

            RESULT_CANCELED -> {
                ToastUtils.show(this, R.string.capture_permission_denied)
                val intent = Intent(ScreenColorPickerCheckpointReceiver.ACTION_PERMISSION_DENIED).apply {
                    putExtra(EXTRA_REQUEST_ID, requestId)
                }
                applicationContext.sendBroadcast(intent)
                finish()
            }
        }
    }

    private fun onPermissionGranted(projectionData: Intent) {
        val serviceIntent = Intent(this, ScreenColorPickerService::class.java).apply {
            putExtra(ScreenColorPickerService.EXTRA_PROJECTION_DATA, projectionData)
            putExtra(ScreenColorPickerService.EXTRA_REQUEST_ID, requestId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        permissionRequestLauncher.launch(Unit)
    }
}
