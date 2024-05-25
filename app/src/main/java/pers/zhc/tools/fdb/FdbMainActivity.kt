package pers.zhc.tools.fdb

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.BuildConfig
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.databinding.FdbMainActivityBinding
import pers.zhc.tools.jni.JNI.ByteSize
import pers.zhc.tools.utils.*
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
            checkAndStartFdb()
        }

        val updateClearCacheButtonText = {
            val sizeString = if (BuildConfig.rustEnableJni) {
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
            createFdbWindow(this).also {
                it.hardwareAcceleration = hardwareAccelerated
                it.startFDB()
            }.showImportPathFilePicker(pathTmpDir)
        }

        val serviceIntent = Intent(this, FdbService::class.java)
        startService(serviceIntent)

        // from file manager "open as"
        checkFromOpenAs()?.let { result ->
            val inputStream = result.openInputStream()
            if (inputStream == null) {
                ToastUtils.show(this, R.string.file_open_failed_toast)
                finish()
                return
            }
            val tmpPathFile = File(cacheDir, System.currentTimeMillis().toString())
            tmpPathFile.outputStream().also {
                val copySizeLimit = 100 * 1048576 /* 100 MiB */
                inputStream.copyTo(it, copySizeLimit)
                it.close()
            }
            inputStream.close()

            checkAndStartFdb()?.showImportPathDialog(tmpPathFile.path)
            finish()
        }
    }

    private fun checkAndStartFdb(): FdbWindow? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null

        return if (!checkDrawOverlayPermission()) {
            launcher.overlaySetting!!.launch(this.packageName)
            null
        } else {
            val fdb = createFdbWindow(this).also {
                it.hardwareAcceleration = hardwareAccelerated
                it.startFDB()
            }
            ToastUtils.show(this, fdb.toString())
            fdb
        }
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
            R.id.create_shortcut -> {
                ShortcutUtils.checkAndToastPinShortcutsSupported(this)
                ShortcutUtils.createStartingActivityPinShortcut(
                    this,
                    StartingFdbActivity::class.java,
                    getString(R.string.fdb_shortcut_start_fdb_label)
                )
            }
        }
        return true
    }

    companion object {
        val fdbMap = HashMap<Long, FdbWindow>()

        fun createFdbWindow(context: Context): FdbWindow {
            return FdbWindow(context).apply {
                fdbMap[this.fdbId] = this
                onExitListener = {
                    fdbMap.remove(this.fdbId)
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.M)
        fun checkDrawOverlayPermission(): Boolean {
            return Settings.canDrawOverlays(MyApplication.appContext)
        }
    }
}
