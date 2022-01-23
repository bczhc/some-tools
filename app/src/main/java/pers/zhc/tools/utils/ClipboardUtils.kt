package pers.zhc.tools.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import pers.zhc.tools.R

/**
 * @author bczhc
 */
class ClipboardUtils {
    companion object {
        fun putWithToast(context: Context, s: String) {
            val cm = context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            cm ?: run {
                ToastUtils.show(context, R.string.clipboard_copy_failed_toast)
                return
            }

            cm.setPrimaryClip(ClipData.newPlainText("text", s))
            ToastUtils.show(context, R.string.clipboard_copy_success_toast)
        }
    }
}