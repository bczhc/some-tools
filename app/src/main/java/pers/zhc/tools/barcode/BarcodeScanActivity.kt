package pers.zhc.tools.barcode

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import pers.zhc.tools.BaseActivity

class BarcodeScanActivity : BaseActivity() {
    private val scanOptions by lazy {
        ScanOptions().apply {
            setPrompt("")
            setOrientationLocked(false)
        }
    }

    private val showResultLauncher: ActivityResultLauncher<ScanResult> =
        registerForActivityResult(BarcodeResultContract()) {
            // back pressed, start scanning again
            barcodeLauncher.launch(scanOptions)
        }

    private val barcodeLauncher: ActivityResultLauncher<ScanOptions> = registerForActivityResult(ScanContract()) {
        it.contents ?: run {
            // cancelled
            finish()
            return@registerForActivityResult
        }
        val scanResult = ScanResult(
            content = it.contents,
            rawBytes = it.rawBytes,
            errorCorrectionLevel = it.errorCorrectionLevel,
            formatName = it.formatName
        )
        showResultLauncher.launch(scanResult)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        barcodeLauncher.launch(scanOptions)
    }
}
