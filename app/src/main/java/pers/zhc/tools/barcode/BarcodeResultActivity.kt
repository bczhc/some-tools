package pers.zhc.tools.barcode

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import io.ktor.util.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.BarcodeScanResultActivityBinding
import pers.zhc.tools.utils.ClipboardUtils
import pers.zhc.tools.utils.getSerializableExtra
import pers.zhc.tools.utils.toHexString
import pers.zhc.tools.utils.toast

class BarcodeResultActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scanResult = intent.getSerializableExtra(EXTRA_SCAN_RESULT, ScanResult::class) ?: run {
            toast(R.string.barcode_no_data_toast)
            return
        }
        val bindings = BarcodeScanResultActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        bindings.contentTv.text = scanResult.content
        bindings.formatName.text = scanResult.formatName
        bindings.errorCorrectionLevelTv.text = scanResult.errorCorrectionLevel
        bindings.rawDataTv.text = scanResult.rawBytes?.let { formatRawData(it) }

        val setUpLongClickListener = { tv: TextView ->
            (tv.parent as LinearLayout).setOnLongClickListener {
                ClipboardUtils.putWithToast(this, tv.text.toString())
                true
            }
        }
        setUpLongClickListener(bindings.contentTv)
        setUpLongClickListener(bindings.formatName)
        setUpLongClickListener(bindings.errorCorrectionLevelTv)
        setUpLongClickListener(bindings.rawDataTv)
    }

    private fun formatRawData(data: ByteArray): String {
        val line1 = data.joinToString { "${it.toUByte()}" }
        val line2 = data.toHexString(" ")
        val line3 = data.encodeBase64()
        return "$line1\n\n$line2\n\n$line3"
    }

    companion object {
        /**
         * Serializable intent extra: [ScanResult]
         */
        const val EXTRA_SCAN_RESULT = "scan result"
    }
}
