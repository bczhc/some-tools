package pers.zhc.tools.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Base64
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.BuildConfig
import pers.zhc.tools.databinding.AppAboutActivityBinding
import pers.zhc.tools.databinding.GitLogViewBinding
import pers.zhc.tools.utils.decompressBzip2

class AboutActivity: BaseActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = AppAboutActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        bindings.showGitLogBtn.setOnClickListener {
            showGitLogDialog()
        }

        bindings.versionNameTv.text = "Version name: " + BuildConfig.VERSION_NAME
        bindings.versionCodeTv.text = "Version code: " + BuildConfig.VERSION_CODE.toString()
    }

    private fun showGitLogDialog() {
        val commitLogSplit = BuildConfig.commitLogEncodedSplit
        val base64Encoded = commitLogSplit.joinToString(separator = "")
        val gitLog = Base64.decode(base64Encoded, Base64.DEFAULT).decompressBzip2().toString(Charsets.UTF_8)

        val bindings = GitLogViewBinding.inflate(layoutInflater)
        bindings.tv.text = gitLog

        Dialog(this).apply {
            setContentView(bindings.root)
        }.show()
    }
}
