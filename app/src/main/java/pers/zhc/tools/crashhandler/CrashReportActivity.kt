package pers.zhc.tools.crashhandler

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.BuildConfig
import pers.zhc.tools.Info
import pers.zhc.tools.R
import pers.zhc.tools.databinding.UncaughtExceptionReportActivityBinding
import pers.zhc.tools.utils.ClipboardUtils
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.toUtcIso8601
import java.util.*

/**
 * @author bczhc
 */
class CrashReportActivity : BaseActivity() {
    private lateinit var uploadStateTextView: TextView
    private lateinit var uploadContent: String
    private lateinit var bindings: UncaughtExceptionReportActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = UncaughtExceptionReportActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)
        uploadStateTextView = bindings.state

        val intent = intent
        val exception = intent.getStringExtra(EXTRA_EXCEPTION_TEXT)
        bindings.content.text = exception ?: ""

        bindings.restartBtn.setOnClickListener {
            val launchIntent = Intent().apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setClass(this@CrashReportActivity, Info.LAUNCHER_CLASS)
            }
            startActivity(launchIntent)
            killProcess()
        }

        val filename = intent.getStringExtra(EXTRA_FILENAME)!!
        val deviceInfo = collectDeviceInfo()
        val timestamp = System.currentTimeMillis()
        val infoLines = mutableListOf(
            "Date: ${Date(timestamp).toUtcIso8601()}",
            "Timestamp: $timestamp"
        )
        infoLines.addAll(deviceInfo.entries.map {
            "${it.key}: ${it.value}"
        })
        val content = infoLines.joinToString("\n") + "\n\n" + exception
        uploadContent = content

        bindings.uploadReportBtn.setOnClickListener {
            upload(content)
        }
        bindings.copyBtn.setOnClickListener {
            ClipboardUtils.putWithToast(this, content)
            // for debugging
            System.err.println(exception)
        }
        try {
            CrashHandler.save2File(this, filename, content)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.show(this, R.string.failed_to_save_file_toast)
        }

        onBackPressedDispatcher.addCallback {
            killProcess()
            finish()
        }
    }

    private fun killProcess() {
        Process.killProcess(Process.myPid())
    }

    /**
     * 收集设备参数信息
     */
    private fun collectDeviceInfo(): Map<String, String> {
        val declaredFields = listOf(*BuildConfig::class.java.declaredFields, *Build::class.java.declaredFields)
        return declaredFields.associate {
            it.isAccessible = true
            val value = runCatching { "${it[null]}" }.getOrElse { "Unknown" }
            Pair(it.name, value)
        }
    }

    private fun upload(information: String) {
        uploadStateTextView.setTextColor(Color.BLUE)
        uploadStateTextView.setText(R.string.uploading)
        CrashReportUploader.upload(this, information, {
            uploadStateTextView.setTextColor(ContextCompat.getColor(this, R.color.done_green))
            uploadStateTextView.setText(R.string.upload_done)
        }) { message ->
            uploadStateTextView.setTextColor(ContextCompat.getColor(this, R.color.red))
            if (message == null) {
                uploadStateTextView.text = getString(R.string.upload_failed_server_error_toast)
            } else {
                uploadStateTextView.text = getString(R.string.upload_failed_toast, message)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        MenuInflater(this).inflate(R.menu.crash_report_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.show_device_info -> {
                bindings.content.text = uploadContent
            }

            else -> {
                return false
            }
        }
        return true
    }

    companion object {
        const val EXTRA_EXCEPTION_TEXT = "exception"

        const val EXTRA_FILENAME = "filename"
    }
}
