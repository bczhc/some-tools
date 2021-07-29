package pers.zhc.tools.media

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import pers.zhc.tools.BaseActivity

/**
 * @author bczhc
 */
class ScreenCapturePermissionRequestActivity : BaseActivity() {

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        setResult(result.resultCode, result.data)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mpm =
            applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mpm.createScreenCaptureIntent()
        launcher.launch(intent)
    }

    companion object {
        fun getRequestLauncher(activity: BaseActivity, callback: (result: ActivityResult?) -> Unit): ActivityResultLauncher<Unit> {
            return activity.registerForActivityResult(object : ActivityResultContract<Unit, ActivityResult?>() {
                override fun createIntent(context: Context, input: Unit): Intent {
                    return Intent(context, ScreenCapturePermissionRequestActivity::class.java)
                }

                override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult? {
                    return ActivityResult(resultCode, intent)
                }
            }) { result ->
                callback(result)
            }
        }
    }
}