package pers.zhc.tools.fdb

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.fdb_main_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.media.ScreenCapturePermissionRequestActivity
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.requireDelete
import java.io.File
import java.io.IOException

/**
 * @author bczhc
 */
class FdbMainActivity : BaseActivity() {
    private lateinit var requestCapturePermissionCallback: ((result: ActivityResult) -> Unit)

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

    private val pathTmpDir by lazy { File(filesDir, "path") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fdb_main_activity)

        val startButton = start_button!!
        val clearCacheButton = clear_cache_btn!!
        val openCacheDirButton = open_cache_btn!!

        startButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!checkDrawOverlayPermission()) {
                    launcher.overlaySetting!!.launch(this.packageName)
                    return@setOnClickListener
                } else {
                    ToastUtils.show(this, createFdbWindow().also { it.startFDB() }.toString())
                }
            } else return@setOnClickListener
        }

        val updateClearCacheButtonText = {
            clearCacheButton.text = getString(R.string.fdb_clear_cache_button, getCacheSize() / 1024)
        }
        updateClearCacheButtonText()
        clearCacheButton.setOnClickListener {
            deleteTmpPathFiles()
            updateClearCacheButtonText()
            ToastUtils.show(this, R.string.fdb_clear_cache_success)
        }
        clearCacheButton.setOnLongClickListener {
            updateClearCacheButtonText()
            return@setOnLongClickListener true
        }

        openCacheDirButton.setOnClickListener {
            createFdbWindow().also { it.startFDB() }.showImportPathDialog(pathTmpDir)
        }

        val serviceIntent = Intent(this, FdbService::class.java)
        startService(serviceIntent)
    }

    private fun createFdbWindow(): FdbWindow {
        return FdbWindow(this).apply {
            fdbMap[this.getFdbId()] = this
            onExitListener = {
                fdbMap.remove(this.getFdbId())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkDrawOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    fun requestCapturePermission(callback: (result: ActivityResult) -> Unit) {
        requestCapturePermissionCallback = callback
        launcher.requestCapturePermission.launch(Unit)
    }

    private fun getCacheFilesNotInUse(): List<File> {
        return (pathTmpDir.listFiles() ?: throw IOException()).filterNot { file ->
            fdbMap.keys.map { it.toString() }.contains(file.nameWithoutExtension)
        }
    }

    private fun getCacheSize(): Long {
        return getCacheFilesNotInUse().sumOf { it.length() }
    }

    private fun deleteTmpPathFiles() {
        getCacheFilesNotInUse().forEach { it.requireDelete() }
    }

    companion object {
        private val fdbMap = HashMap<Long, FdbWindow>()
    }
}
