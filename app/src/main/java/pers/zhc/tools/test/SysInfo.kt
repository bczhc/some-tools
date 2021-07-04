package pers.zhc.tools.test

import android.os.Bundle
import kotlinx.android.synthetic.main.sys_info_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.jni.JNI
import java.util.*

/**
 * @author bczhc
 */
class SysInfo: BaseActivity() {
    private var runFlag = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sys_info_activity)

        val uptimeTV = uptime_tv!!
        val processesTV = processes_tv!!

        val calender = Calendar.getInstance()

        Thread {
            while (runFlag) {
                calender.set(0, 0, 0, 0, 0, JNI.SysInfo.getUptime().toInt())
                val h = calender.get(Calendar.HOUR_OF_DAY)
                val m = calender.get(Calendar.MINUTE)
                val s = calender.get(Calendar.SECOND)

                runOnUiThread {
                    uptimeTV.text = getString(R.string.uptime_is_, getTimeString(h, m, s))
                    processesTV.text = getString(R.string.processes_count_is_, JNI.SysInfo.getProcessesCount())
                }

                Thread.sleep(1000)
            }
        }.start()
    }

    private fun complete(x: Int): String {
        var s = x.toString()
        if (s.length == 1) {
            s = "0$s"
        }
        return s
    }

    private fun getTimeString(h: Int, m: Int, s: Int): String {
        return "${complete(h)}:${complete(m)}:${complete(s)}"
    }

    override fun finish() {
        runFlag = false
        super.finish()
    }
}