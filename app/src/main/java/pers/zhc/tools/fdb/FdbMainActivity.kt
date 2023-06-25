package pers.zhc.tools.fdb

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.BuildConfig
import pers.zhc.tools.R
import pers.zhc.tools.databinding.FdbMainActivityBinding
import pers.zhc.tools.databinding.FdbPathImportPromptDialogBinding
import pers.zhc.tools.databinding.FdbPathImportWindowBinding
import pers.zhc.tools.floatingdrawing.PaintView
import pers.zhc.tools.jni.JNI.ByteSize
import pers.zhc.tools.utils.AsyncTryDo
import pers.zhc.tools.utils.DialogUtils
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.requireDelete
import pers.zhc.tools.utils.requireMkdir
import pers.zhc.tools.utils.runOnUiThread
import pers.zhc.tools.utils.setNegativeAction
import pers.zhc.tools.utils.setPositiveAction
import java.io.File
import java.io.IOException


/**
 * @author bczhc
 */
class FdbMainActivity : BaseActivity() {
    private var hardwareAccelerated = false

    private val launcher = object {
        val overlaySetting = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerForActivityResult(OverlaySettingContract()) {}
        } else {
            null
        }
    }

    private val pathTmpDir by lazy { File(filesDir, "path").also { it.requireMkdir() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = FdbMainActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val startButton = bindings.startButton
        val clearCacheButton = bindings.clearCacheBtn
        val openCacheDirButton = bindings.openCacheBtn
        startButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!checkDrawOverlayPermission()) {
                    launcher.overlaySetting!!.launch(this.packageName)
                    return@setOnClickListener
                } else {
                    ToastUtils.show(this, createFdbWindow().also {
                        it.hardwareAcceleration = hardwareAccelerated
                        it.startFDB()
                    }.toString())
                }
            } else return@setOnClickListener
        }

        val updateClearCacheButtonText = {
            val sizeString = if (!BuildConfig.rustDisabled) {
                ByteSize.toHumanReadable(getCacheSize(), true)
            } else {
                getCacheSize().toString()
            }
            clearCacheButton.text =
                getString(R.string.fdb_clear_cache_button, sizeString)
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
            createFdbWindow().also {
                it.hardwareAcceleration = hardwareAccelerated
                it.startFDB()
            }.showImportPathDialog(pathTmpDir)
        }

        val serviceIntent = Intent(this, FdbService::class.java)
        startService(serviceIntent)
        if (Intent.ACTION_VIEW == intent?.action) {
            val uri: Uri? = intent.data
            if(uri != null) {
                var pathFile = File(uri.path)
                val pathStr = (pathFile.canonicalPath.replace("/root","")).replace("/document/primary:","/storage/emulated/0/")
                ToastUtils.show(this, pathStr)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!checkDrawOverlayPermission()) {
                        launcher.overlaySetting!!.launch(this.packageName)
                    } else {
                        ToastUtils.show(this, createFdbWindow().also {
                            it.hardwareAcceleration = hardwareAccelerated
                            it.startFDB()
                        }.toString())
                    }
                } else return
                FdbWindow(this as Context).importPath(pathStr)
            }
        }
    }

    private fun createFdbWindow(): FdbWindow {
        return FdbWindow(this).apply {
            fdbMap[this.fdbId] = this
            onExitListener = {
                fdbMap.remove(this.fdbId)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkDrawOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.fdb_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.hardware_acceleration -> {
                item.isChecked = !item.isChecked
                hardwareAccelerated = item.isChecked
            }
        }
        return true
    }

    companion object {
        private val fdbMap = HashMap<Long, FdbWindow>()
    }
}
