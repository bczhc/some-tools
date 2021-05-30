package pers.zhc.tools.utils

import android.content.Context
import android.view.View
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu

/**
 * @author bczhc
 */
class PopupMenuUtil {
    companion object {
        @JvmStatic
        fun createPopupMenu(ctx: Context, anchor: View, @MenuRes menuRes: Int): PopupMenu {
            val popupMenu = PopupMenu(ctx, anchor)
            popupMenu.menuInflater.inflate(menuRes, popupMenu.menu)
            return popupMenu
        }
    }
}