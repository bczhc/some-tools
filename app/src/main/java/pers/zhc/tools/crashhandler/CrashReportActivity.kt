package pers.zhc.tools.crashhandler

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.BuildConfig
import pers.zhc.tools.Info
import pers.zhc.tools.R
import pers.zhc.tools.crashhandler.CrashReportUploader.upload
import pers.zhc.tools.utils.ToastUtils
import java.lang.reflect.Field
import java.util.*

/**
 * @author bczhc
 */
class CrashReportActivity : BaseActivity() {
    private var uploadStateTextView: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ToastUtils.show(this, R.string.no_write_permission)
        }
        val intent = intent
        setContentView(R.layout.uncaught_exception_report_activity)
        val exception = intent.getStringExtra("exception")
        val textView = findViewById<TextView>(R.id.content)
        textView.text = exception
        val restartButton = findViewById<Button>(R.id.restart_btn)
        val uploadReportButton = findViewById<Button>(R.id.upload_report_btn)
        val copyBtn = findViewById<Button>(R.id.copy_btn)
        uploadStateTextView = findViewById(R.id.state)
        restartButton.setOnClickListener { v: View? ->
            val launchIntent = Intent()
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setClass(this, Info.LAUNCHER_CLASS)
            startActivity(launchIntent)
            killProcess()
        }
        val filename = intent.getStringExtra("filename")
        val deviceInfo = collectDeviceInfo()
        val sb = StringBuilder()
        val keySet = deviceInfo.keys
        for (key in keySet) {
            sb.append(key).append(": ").append(deviceInfo[key]).append('\n')
        }
        sb.append(exception)
        val content = sb.toString()
        uploadReportButton.setOnClickListener { v: View? -> upload(filename, content) }
        copyBtn.setOnClickListener { v: View? ->
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            if (cm != null) {
                val cd = ClipData.newPlainText("exception", content)
                cm.setPrimaryClip(cd)
                ToastUtils.show(this, R.string.copying_succeeded)
            }
            // for debugging
            System.err.println(exception)
        }
        CrashHandler.save2File(this, filename, content)
    }

    private fun killProcess() {
        Process.killProcess(Process.myPid())
    }

    /**
     * 收集设备参数信息
     */
    fun collectDeviceInfo(): Map<String, String> {
        val info: MutableMap<String, String> = HashMap(16)
        val declaredFields: MutableList<Field> = ArrayList()
        declaredFields.addAll(Arrays.asList(*BuildConfig::class.java.declaredFields))
        declaredFields.addAll(Arrays.asList(*Build::class.java.declaredFields))
        for (field in declaredFields) {
            field.isAccessible = true
            var o: Any? = null
            try {
                o = field[null]
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
            info[field.name] = o?.toString() ?: "null"
        }
        return info
    }

    private fun upload(@Suppress("unused") filename: String?, information: String) {
        uploadStateTextView!!.setTextColor(Color.BLUE)
        uploadStateTextView!!.setText(R.string.uploading)
        upload(this, information, {
            uploadStateTextView!!.setTextColor(ContextCompat.getColor(this, R.color.done_green))
            uploadStateTextView!!.setText(R.string.upload_done)
            Unit
        }) { message: String? ->
            uploadStateTextView!!.setTextColor(ContextCompat.getColor(this, R.color.red))
            if (message == null) {
                uploadStateTextView!!.text = getString(R.string.upload_failed_server_error_toast)
            } else {
                uploadStateTextView!!.text = getString(R.string.upload_failed_toast, message)
            }
            Unit
        }
    }

    override fun onBackPressed() {
        killProcess()
        super.onBackPressed()
    }
}
