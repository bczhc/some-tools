package pers.zhc.tools.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Base64
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.BuildConfig
import pers.zhc.tools.databinding.AppAboutActivityBinding
import pers.zhc.tools.databinding.GitLogViewBinding
import pers.zhc.tools.utils.decompressBzip2

class AboutActivity : BaseActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = AppAboutActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        bindings.showGitLogBtn.setOnClickListener {
            showGitLogDialog()
        }

        bindings.infoTv.text = """Build type: ${BuildConfig.BUILD_TYPE}
            |Debuggable: ${BuildConfig.DEBUG}
            |Application ID: ${BuildConfig.APPLICATION_ID}
            |Build SDK version: ${Build.VERSION.SDK_INT}
            |
        """.trimMargin() + decodeLongEncodedString(BuildConfig.buildInfoMessageEncoded)
    }

    private fun showGitLogDialog() {
        val bindings = GitLogViewBinding.inflate(layoutInflater)
        bindings.tv.text = decodeLongEncodedString(BuildConfig.commitLogEncodedSplit, true)

        Dialog(this).apply {
            setContentView(bindings.root)
        }.show()
    }

    private fun decodeLongEncodedString(encoded: Array<String>, bz2Compressed: Boolean = false): String {
        return Base64.decode(encoded.joinToString(separator = ""), Base64.DEFAULT)
            .let {
                if (bz2Compressed) {
                    it.decompressBzip2()
                } else {
                    it
                }
            }.toString(Charsets.UTF_8)
    }
}
