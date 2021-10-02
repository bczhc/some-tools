package pers.zhc.tools.utils

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import pers.zhc.tools.views.ProgressView

/**
 * @author bczhc
 */
class ProgressDialog(context: Context) : Dialog(context) {
    private val progressView: ProgressView = ProgressView(context)

    init {
        setContentView(progressView, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    fun getProgressView(): ProgressView {
        return progressView
    }
}