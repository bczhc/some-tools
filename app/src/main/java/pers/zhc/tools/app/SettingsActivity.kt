package pers.zhc.tools.app

import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AppCompatDelegate.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.BuildConfig
import pers.zhc.tools.R
import pers.zhc.tools.app.Settings.Companion.AppTheme
import pers.zhc.tools.databinding.MainActivityBinding
import pers.zhc.tools.filepicker.FilePickerActivityContract
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.*
import java.io.File

/**
 * @author bczhc
 */
class SettingsActivity : BaseActivity() {
    private val dataTransfer = object {
        val importFileLauncher = registerForActivityResult(
            FilePickerActivityContract(FilePickerActivityContract.FilePickerType.PICK_FILE, false)
        ) {
            it ?: return@registerForActivityResult

            determinateProgressDialog(
                this@SettingsActivity,
                getString(R.string.importing_dialog_title)
            ) { updateText, updateProgress, finish ->
                thread {
                    val cacheFile = tmpFile()
                    File(it.path).copyTo(cacheFile, overwrite = true)

                    val externalFilesDir = externalFilesDir().apply {
                        deleteRecursively()
                        mkdir()
                    }
                    val internalFilesDir = filesDir.apply {
                        deleteRecursively()
                        mkdir()
                    }

                    JNI.App.extractAppData(
                        cacheFile.path,
                        internalFilesDir.path,
                        externalFilesDir.path
                    ) { n, total, name ->
                        updateText("$n/$total\n$name")
                        updateProgress(n.toFloat() / total.toFloat())
                    }

                    cacheFile.requireDelete()
                    finish()
                    runOnUiThread {
                        MaterialAlertDialogBuilder(this@SettingsActivity)
                            .setTitle(R.string.restart_title)
                            .setMessage(R.string.settings_import_data_restart_dialog_msg)
                            .setPositiveAction { _, _ ->
                                Process.killProcess(Process.myPid())
                            }
                            .create().apply {
                                setCancelable(false)
                                setCanceledOnTouchOutside(false)
                            }.show()
                    }
                }
            }
        }

        val exportFileLauncher = registerForActivityResult(
            FilePickerActivityContract(FilePickerActivityContract.FilePickerType.PICK_FOLDER, true)
        ) {
            it ?: return@registerForActivityResult
            it.filename ?: return@registerForActivityResult

            determinateProgressDialog(
                this@SettingsActivity,
                getString(R.string.exporting_dialog_title)
            ) { updateText, updateProgress, finish ->
                thread {
                    val outputFile = File(it.path, it.filename).checkAddExtension("bak")
                    // prevent recursive archiving if `outputFile` is chosen at `externalFilesDir()`...
                    val cacheFile = tmpFile()
                    val level = if (BuildConfig.ndkReleaseBuild) 5 else 2
                    JNI.App.archiveAppData(
                        cacheFile.path,
                        filesDir.path,
                        externalFilesDir().path,
                        level
                    ) { n, total, name ->
                        updateText("$n/$total\n$name")
                        updateProgress(n.toFloat() / total.toFloat())
                    }
                    cacheFile.copyTo(outputFile, overwrite = true)
                    cacheFile.requireDelete()
                    finish()
                    ToastUtils.show(this@SettingsActivity, R.string.exporting_succeeded)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = MainActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        bindings.setUpUrlSettings()
        bindings.setUpDataTransferSettings()
        bindings.setUpThemeSettings()
    }

    private fun MainActivityBinding.setUpUrlSettings() {
        val bindings = this
        val serverET = bindings.serverEt
        val resourceET = bindings.resourceEt
        val githubET = bindings.githubRawUrlEt
        val saveBtn = bindings.save

        val settings = Settings.readSettings()
        val appServerUrl = settings.serverUrl ?: Settings.Companion.AppServerUrl.default()

        serverET.editText.setText(appServerUrl.serverRootUrl)
        resourceET.editText.setText(appServerUrl.staticSourceRootUrl)
        githubET.editText.setText(appServerUrl.githubRawRootUrl)

        saveBtn.setOnClickListener {
            val newServerURL = serverET.editText.text.toString()
            val newResourceURL = resourceET.editText.text.toString()
            val newGithubRawURL = githubET.editText.text.toString()

            Settings.updateSettings {
                it.serverUrl = Settings.Companion.AppServerUrl(
                    serverRootUrl = newServerURL,
                    staticSourceRootUrl = newResourceURL,
                    githubRawRootUrl = newGithubRawURL,
                )
            }

            ToastUtils.show(this@SettingsActivity, R.string.saved)
        }
    }

    private fun MainActivityBinding.setUpDataTransferSettings() {
        importBtn.setOnClickListener {
            dataTransfer.importFileLauncher.launch(Unit)
        }

        exportBtn.setOnClickListener {
            dataTransfer.exportFileLauncher.launch(Unit)
        }
    }

    private fun MainActivityBinding.setUpThemeSettings() {
        when (getDefaultNightMode()) {
            MODE_NIGHT_YES -> R.id.dark
            MODE_NIGHT_NO -> R.id.light
            MODE_NIGHT_FOLLOW_SYSTEM, MODE_NIGHT_UNSPECIFIED -> R.id.follow_system
            else -> null
        }?.let { themeRg.check(it) }
        themeRg.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.dark -> {
                    MODE_NIGHT_YES
                }

                R.id.light -> {
                    MODE_NIGHT_NO
                }

                R.id.follow_system -> {
                    MODE_NIGHT_FOLLOW_SYSTEM
                }

                else -> unreachable()
            }

            val appTheme = AppTheme.fromNightModeOption(mode)
            Settings.updateSettings {
                it.theme = appTheme
            }

            setDefaultNightMode(mode)
        }
    }
}
