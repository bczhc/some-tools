package pers.zhc.tools.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import pers.zhc.tools.BuildConfig

/**
 * input: package
 */
@RequiresApi(Build.VERSION_CODES.R)
class AllFilesAccessPermissionRequestContract : ActivityResultContract<String?, Unit>() {
    override fun createIntent(context: Context, input: String?): Intent {
        return if (input != null) {
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            )
        } else {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {
    }
}
