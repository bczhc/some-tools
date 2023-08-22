package pers.zhc.tools.filepicker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import org.jetbrains.annotations.Contract
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.AllFilesAccessPermissionRequestContract
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.PermissionRequester
import pers.zhc.tools.utils.ToastUtils
import java.io.File

/**
 * To use this activity, use [FilePickerActivityContract].
 */
class FilePicker : BaseActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    private val requestAllFilesAccessLauncher = registerForActivityResult(
        AllFilesAccessPermissionRequestContract()
    ) {
        if (Environment.isExternalStorageManager()) {
            // permission granted
            initializePicker()
        } else {
            ToastUtils.show(this, R.string.please_grant_permission)
            finish()
        }
    }

    private var filePickerRL: FilePickerRL? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // after API 30, request "all files access" permission
            if (Environment.isExternalStorageManager()) {
                initializePicker()
            } else {
                requestAllFilesAccessLauncher.launch(null)
            }
        } else {
            // legacy storage permission
            PermissionRequester { initializePicker() }.requestPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE, RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == 0) initializePicker() else {
                ToastUtils.show(this, R.string.please_grant_permission)
                finish()
            }
        }
    }

    private fun initializePicker() {
        val intent = intent
        var initialPath = intent.getStringExtra("initialPath")
        val enableEditText = intent.getBooleanExtra(EXTRA_ENABLE_FILENAME, false)
        initialPath = initialPath ?: Common.getExternalStoragePath(this)
        val initFilename = intent.getStringExtra(EXTRA_DEFAULT_FILENAME)
        filePickerRL =
            FilePickerRL(this, intent.getIntExtra(EXTRA_OPTION, PICK_FILE), File(initialPath), { _ ->
                finish()
                overridePendingTransition(0, R.anim.fade_out)
            }, { picker: FilePickerRL, path: String? ->
                val r = Intent()
                r.putExtra(EXTRA_RESULT, path)
                if (enableEditText) r.putExtra(EXTRA_FILENAME_RESULT, picker.filenameText)
                setResult(RESULT_CODE, r)
                finish()
                overridePendingTransition(0, R.anim.fade_out)
            }, initFilename, enableEditText)
        setContentView(filePickerRL)

        onBackPressedDispatcher.addCallback {
            filePickerRL!!.previous()
        }
    }

    private class Result @Contract(pure = true) constructor(val path: String?, filename: String?) {
        val filename: String

        init {
            this.filename = filename!!
        }
    }

    companion object {
        const val PICK_FILE = FilePickerRL.TYPE_PICK_FILE
        const val PICK_FOLDER = FilePickerRL.TYPE_PICK_FOLDER
        const val RESULT_CODE = 0
        const val EXTRA_OPTION = "option"
        const val EXTRA_RESULT = "result"
        const val EXTRA_ENABLE_FILENAME = "enableFilename"
        const val EXTRA_FILENAME_RESULT = "filenameResult"

        /**
         * string extra
         */
        const val EXTRA_DEFAULT_FILENAME = "defaultFilename"

        @Contract("_, _ -> new")
        fun getLauncher(activity: BaseActivity, callback: (path: String?) -> Unit): ActivityResultLauncher<Int> {
            return activity.registerForActivityResult(object : ActivityResultContract<Int, String?>() {
                override fun createIntent(context: Context, input: Int): Intent {
                    val intent = Intent(activity, FilePicker::class.java)
                    intent.putExtra(EXTRA_OPTION, input)
                    return intent
                }

                override fun parseResult(resultCode: Int, intent: Intent?): String? {
                    return if (intent == null) null else intent.getStringExtra(EXTRA_RESULT)!!
                }
            }) { path: String? -> callback(path) }
        }

        @Contract("_, _ -> new")
        fun getLauncherWithFilename(
            activity: BaseActivity,
            defaultFilename: String = "",
            callback: (path: String?, filename: String) -> Unit
        ): ActivityResultLauncher<Int> {
            return activity.registerForActivityResult(object : ActivityResultContract<Int, Result?>() {
                override fun createIntent(context: Context, input: Int): Intent {
                    val intent = Intent(activity, FilePicker::class.java)
                    intent.putExtra(EXTRA_OPTION, input)
                    intent.putExtra(EXTRA_ENABLE_FILENAME, true)
                    intent.putExtra(EXTRA_DEFAULT_FILENAME, defaultFilename)
                    return intent
                }

                override fun parseResult(resultCode: Int, intent: Intent?): Result? {
                    return if (intent == null) null else Result(
                        intent.getStringExtra(EXTRA_RESULT),
                        intent.getStringExtra(EXTRA_FILENAME_RESULT)
                    )
                }
            }) { result: Result? -> callback(result?.path, result!!.filename) }
        }
    }
}
