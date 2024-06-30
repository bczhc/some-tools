package pers.zhc.tools.barcode

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class BarcodeResultContract: ActivityResultContract<ScanResult, Unit>() {
    override fun createIntent(context: Context, input: ScanResult): Intent {
        return Intent(context, BarcodeResultActivity::class.java).apply {
            putExtra(BarcodeResultActivity.EXTRA_SCAN_RESULT, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {
    }
}
