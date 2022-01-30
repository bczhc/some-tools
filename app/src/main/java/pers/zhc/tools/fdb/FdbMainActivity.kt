package pers.zhc.tools.fdb

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.fdb_main_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.media.ScreenCapturePermissionRequestActivity
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class FdbMainActivity : BaseActivity() {

    private lateinit var requestCapturePermissionCallback: ((result: ActivityResult) -> Unit)

    companion object {
        @SuppressLint("StaticFieldLeak")
        var debugTV: TextView? = null
    }

    private val launcher = object {
        val overlaySetting = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerForActivityResult(OverlaySettingContract()) {}
        } else {
            null
        }

        val requestCapturePermission =
            ScreenCapturePermissionRequestActivity.getRequestLauncher(this@FdbMainActivity) { result ->
                requestCapturePermissionCallback(result!!)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fdb_main_activity)

        debugTV = debug_tv!!

        val run = {
            val fdbWindow = FdbWindow(this)
            ToastUtils.show(this, fdbWindow.toString())
            fdbWindow.startFDB()
        }

        val startButton = start_button!!
        startButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!checkDrawOverlayPermission()) {
                    launcher.overlaySetting!!.launch(this.packageName)
                    return@setOnClickListener
                } else run()
            } else return@setOnClickListener
        }

        val serviceIntent = Intent(this, FdbService::class.java)
        startService(serviceIntent)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkDrawOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    fun requestCapturePermission(callback: (result: ActivityResult) -> Unit) {
        requestCapturePermissionCallback = callback
        launcher.requestCapturePermission.launch(Unit)
    }

    override fun finish() {
        debugTV = null
        super.finish()
    }
}