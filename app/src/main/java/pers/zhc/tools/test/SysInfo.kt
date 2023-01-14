package pers.zhc.tools.test

import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.SysInfoActivityBinding
import pers.zhc.tools.jni.JNI

/**
 * @author bczhc
 */
class SysInfo : BaseActivity() {
    private var runFlag = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = SysInfoActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val uptimeTV = bindings.uptimeTv
        val processesTV = bindings.processesTv

        Thread {
            while (runFlag) {
                val uptime = JNI.SysInfo.getUptime().toInt()
                val d = uptime / 3600 / 24
                val h = uptime / 3600 - d * 24
                val m = uptime / 60 - d * 24 * 60 - h * 60
                val s = uptime - d * 24 * 3600 - h * 3600 - m * 60

                runOnUiThread {
                    uptimeTV.text = getString(R.string.uptime_is_, getTimeString(d, h, m, s))
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

    private fun getTimeString(d: Int, h: Int, m: Int, s: Int): String {
        val sb = StringBuilder()
        if (d == 1) {
            sb.append("$d day, ")
        } else if (d > 1) {
            sb.append("$d days, ")
        }
        sb.append("${complete(h)}:${complete(m)}:${complete(s)}")
        return sb.toString()
    }

    override fun finish() {
        runFlag = false
        super.finish()
    }
}