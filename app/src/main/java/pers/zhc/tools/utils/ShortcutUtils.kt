package pers.zhc.tools.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import pers.zhc.tools.R

object ShortcutUtils {
    fun checkAndToastPinShortcutsSupported(context: Context) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            ToastUtils.show(context, R.string.words_not_support_pinned_shortcut_toast)
            return
        }
    }

    fun createStartingActivityPinShortcut(
        context: Context,
        `class`: Class<*>,
        shortLabel: String,
        id: String = "shortcut.pin.class-${`class`.canonicalName}"
    ) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return
        }

        val intent = Intent("NONE_ACTION")
        intent.setClass(context, `class`)
        val shortcut = ShortcutInfoCompat.Builder(context, id).apply {
            setIcon(IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground))
            setIntent(intent)
            setShortLabel(shortLabel)
        }.build()
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }
}
