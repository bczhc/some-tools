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
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.indeterminateProgressDialog
import pers.zhc.tools.utils.setPositiveAction
import pers.zhc.tools.utils.thread
import kotlin.io.path.Path
import kotlin.io.path.pathString

/**
 * @author bczhc
 */
class Settings : BaseActivity() {
    private val dataTransfer = object {
        val importFileLauncher = registerForActivityResult(
            FilePickerActivityContract(FilePickerActivityContract.FilePickerType.PICK_FILE, false)
        ) {
            it ?: return@registerForActivityResult

            indeterminateProgressDialog(this@Settings, getString(R.string.extracting_archive_msg)) { finish ->
                thread {
                    val filesDir = filesDir
                    filesDir.deleteRecursively()
                    filesDir.mkdir()

                    JNI.Compression.extractTarBz3(it.path, filesDir.path)

                    finish()
                    runOnUiThread {
                        MaterialAlertDialogBuilder(this@Settings)
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

            indeterminateProgressDialog(this@Settings, getString(R.string.creating_archive_msg)) { finish ->
                thread {
                    val outputFile = Path(it.path, it.filename)
                    JNI.Compression.createTarBz3(filesDir.path, outputFile.pathString)
                    finish()
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

            ToastUtils.show(this@Settings, R.string.saved)
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
