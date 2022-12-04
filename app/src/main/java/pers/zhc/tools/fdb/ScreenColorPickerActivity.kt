package pers.zhc.tools.fdb

import android.content.Intent
import android.os.Build
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.fdb.FdbBroadcastReceiver.Companion.ACTION_ON_CAPTURE_SCREEN_PERMISSION_DENIED
import pers.zhc.tools.fdb.FdbBroadcastReceiver.Companion.ACTION_ON_CAPTURE_SCREEN_PERMISSION_GRANTED
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.MediaUtils

/**
 * @author bczhc
 */
class ScreenColorPickerActivity : BaseActivity() {
    private var fdbId = 0L

    private val permissionRequestLauncher = registerForActivityResult(
        MediaUtils.createCapturePermissionContract()
    ) { result ->
        result!!
        if (result.resultCode == RESULT_OK) {
            val intent = Intent(ACTION_ON_CAPTURE_SCREEN_PERMISSION_GRANTED)
            intent.putExtra(FdbBroadcastReceiver.EXTRA_FDB_ID, fdbId)
            applicationContext.sendBroadcast(intent)

            onPermissionGranted(result.data!!)

            finish()
        } else {
            ToastUtils.show(this, R.string.capture_permission_denied)

            val intent = Intent(ACTION_ON_CAPTURE_SCREEN_PERMISSION_DENIED)
            intent.putExtra(FdbBroadcastReceiver.EXTRA_FDB_ID, fdbId)
            applicationContext.sendBroadcast(intent)
            finish()
        }
    }

    private fun onPermissionGranted(projectionData: Intent) {
        val serviceIntent = Intent(this, ScreenColorPickerService::class.java)
        serviceIntent.putExtra(ScreenColorPickerService.EXTRA_FDB_ID, fdbId)
        serviceIntent.putExtra(ScreenColorPickerService.EXTRA_PROJECTION_DATA, projectionData)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        val intent = Intent(StartScreenColorPickerReceiver.ACTION_SCREEN_COLOR_PICKER_ON_STARTED)
        intent.putExtra(StartScreenColorPickerReceiver.EXTRA_FDB_ID, fdbId)
        applicationContext.sendBroadcast(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        fdbId = intent.getLongExtra(EXTRA_FDB_ID, 0L)

        // show the permission requesting dialog
        permissionRequestLauncher.launch(Unit)
    }

    companion object {
        /**
         * intent long extra
         */
        const val EXTRA_FDB_ID = "fdbID"
    }
}