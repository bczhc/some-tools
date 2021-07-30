package pers.zhc.tools.fdb

import android.content.Intent
import android.os.Build
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.fdb.FdbBroadcastReceiver.Companion.ACTION_ON_CAPTURE_SCREEN_PERMISSION_DENIED
import pers.zhc.tools.fdb.FdbBroadcastReceiver.Companion.ACTION_ON_CAPTURE_SCREEN_PERMISSION_GRANTED
import pers.zhc.tools.media.ScreenCapturePermissionRequestActivity
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class ScreenColorPickerActivity : BaseActivity() {
    private var fdbId = 0L

    /**
     * When isn't null, the screen capture permission has been granted
     */
    private var projectionData: Intent? = null

    private val captureRequestLauncher = ScreenCapturePermissionRequestActivity.getRequestLauncher(this) { result ->
        result!!
        if (result.resultCode == RESULT_OK) {
            projectionData = result.data

            onPermissionGranted()

            val intent = Intent(ACTION_ON_CAPTURE_SCREEN_PERMISSION_GRANTED)
            intent.putExtra(FdbBroadcastReceiver.EXTRA_FDB_ID, fdbId)
            applicationContext.sendBroadcast(intent)
            finish()
        } else {
            ToastUtils.show(this, R.string.capture_permission_denied)

            val intent = Intent(ACTION_ON_CAPTURE_SCREEN_PERMISSION_DENIED)
            intent.putExtra(FdbBroadcastReceiver.EXTRA_FDB_ID, fdbId)
            applicationContext.sendBroadcast(intent)
            finish()
        }
    }

    private fun onPermissionGranted() {
        val serviceIntent = Intent(this, ScreenColorPickerService::class.java)
        serviceIntent.putExtra(ScreenColorPickerService.EXTRA_FDB_ID, fdbId)
        serviceIntent.putExtra(ScreenColorPickerService.EXTRA_PROJECTION_DATA, projectionData!!)
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

        // get the permission
        captureRequestLauncher.launch(Unit)
    }

    companion object {
        /**
         * intent long extra
         */
        const val EXTRA_FDB_ID = "fdbID"
    }
}