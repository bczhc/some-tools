package pers.zhc.tools.fdb

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.fdb_main_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.fdb.FdbInteractionReceiver.Companion.ACTION_START_FAB
import pers.zhc.tools.fdb.FdbInteractionReceiver.Companion.ACTION_STOP_FAB

/**
 * @author bczhc
 */
class FdbMainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fdb_main_activity)

        val fdbSwitch = fdb_switch!!

        // TODO: 7/9/21 handle closing it
        startService(Intent(this, FdbService::class.java))

        fdbSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startFdb()
            } else {
                stopFdb()
            }
        }
    }

    private fun startFdb() {
        val intent = Intent(ACTION_START_FAB)
        sendBroadcast(intent)
    }

    private fun stopFdb() {
        val intent = Intent(ACTION_STOP_FAB)
        sendBroadcast(intent)
    }
}