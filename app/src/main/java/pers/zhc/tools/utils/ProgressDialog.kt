package pers.zhc.tools.utils

import android.app.Dialog
import android.content.Context
import pers.zhc.tools.views.ProgressView

/**
 * @author bczhc
 */
class ProgressDialog(context: Context) : Dialog(context) {
    private val progressView: ProgressView = ProgressView(context)

    init {
        setContentView(progressView)
    }

    fun getProgressView(): ProgressView {
        return progressView
    }
}