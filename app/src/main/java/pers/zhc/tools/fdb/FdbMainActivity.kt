package pers.zhc.tools.fdb

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MotionEvent
import kotlinx.android.synthetic.main.fdb_main_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.fdb.FdbBroadcastReceiver.Companion.ACTION_ON_SCREEN_ORIENTATION_CHANGED
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class FdbMainActivity : BaseActivity() {

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fdb_main_activity)

        val startButton = start_button!!
        startButton.setOnClickListener {
            val fdbWindow = FdbWindow(this)
            ToastUtils.show(this, fdbWindow.toString())
            fdbWindow.startFDB()
        }

        val serviceIntent = Intent(this, FdbService::class.java)
        startService(serviceIntent)
    }
}