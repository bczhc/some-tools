package pers.zhc.tools

import android.os.Bundle
import android.os.Process
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import pers.zhc.tools.MyApplication.Companion.InfoJson.Companion.KEY_GITHUB_RAW_ROOT_URL
import pers.zhc.tools.MyApplication.Companion.InfoJson.Companion.KEY_SERVER_ROOT_URL
import pers.zhc.tools.MyApplication.Companion.InfoJson.Companion.KEY_STATIC_RESOURCE_ROOT_URL
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
    }

    private fun MainActivityBinding.setUpUrlSettings() {
        val bindings = this
        val serverET = bindings.serverEt
        val resourceET = bindings.resourceEt
        val githubET = bindings.githubRawUrlEt
        val saveBtn = bindings.save

        serverET.editText.setText(Info.serverRootURL)
        resourceET.editText.setText(Info.staticResourceRootURL)
        githubET.editText.setText(Info.githubRawRootURL)

        saveBtn.setOnClickListener {
            val jsonObject = JSONObject()
            val newServerURL = serverET.editText.text.toString()
            val newResourceURL = resourceET.editText.text.toString()
            val newGithubRawURL = githubET.editText.text.toString()
            jsonObject.put(KEY_SERVER_ROOT_URL, newServerURL)
            jsonObject.put(KEY_STATIC_RESOURCE_ROOT_URL, newResourceURL)
            jsonObject.put(KEY_GITHUB_RAW_ROOT_URL, newGithubRawURL)
            MyApplication.writeInfoJSON(jsonObject)


            Info.serverRootURL = newServerURL
            Info.staticResourceRootURL = newResourceURL
            Info.githubRawRootURL = newGithubRawURL

            ToastUtils.show(this@SettingsActivity, R.string.saved)
            finish()
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
}
