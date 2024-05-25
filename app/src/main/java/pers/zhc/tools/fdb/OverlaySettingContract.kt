package pers.zhc.tools.fdb

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi

/**
 * @author bczhc
 */
@RequiresApi(Build.VERSION_CODES.M)
class OverlaySettingContract : ActivityResultContract<String?, Unit>() {
    override fun createIntent(context: Context, input: String?): Intent {
        val `package` = input ?: context.packageName
        return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$`package`"))
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {
    }
}
