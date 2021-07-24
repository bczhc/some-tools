package pers.zhc.tools.utils

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

/**
 * @author bczhc
 */
class DialogUtils {
    companion object {
        fun createConfirmationAlertDialog(
            ctx: Context,
            positiveAction: DialogInterface.OnClickListener,
            negativeAction: DialogInterface.OnClickListener = DialogInterface.OnClickListener { _, _ -> },
            view: View? = null,
            title: String,
            width: Int = WRAP_CONTENT,
            height: Int = WRAP_CONTENT,
            applicationOverlay: Boolean = false
        ): AlertDialog {
            return DialogUtil.createConfirmationAlertDialog(
                ctx,
                positiveAction,
                negativeAction,
                view,
                title,
                width,
                height,
                applicationOverlay
            )
        }

        fun createConfirmationAlertDialog(
            ctx: Context,
            positiveAction: DialogInterface.OnClickListener,
            negativeAction: DialogInterface.OnClickListener = DialogInterface.OnClickListener { _, _ -> },
            view: View? = null,
            @StringRes titleRes: Int,
            width: Int = WRAP_CONTENT,
            height: Int = WRAP_CONTENT,
            applicationOverlay: Boolean = false
        ): AlertDialog {
            return DialogUtil.createConfirmationAlertDialog(
                ctx,
                positiveAction,
                negativeAction,
                view,
                ctx.getString(titleRes),
                width,
                height,
                applicationOverlay
            )
        }

        fun createPromptDialog(
            ctx: Context,
            title: String,
            positiveAction: PromptDialogCallback,
            negativeAction: PromptDialogCallback = { _, _ -> },
            editText: EditText = EditText(ctx)
        ): AlertDialog {
            return createConfirmationAlertDialog(ctx, { d, _ ->
                positiveAction(d, editText)
            }, { d, _ ->
                negativeAction(d, editText)
            }, editText, title)
        }

        fun createPromptDialog(
            ctx: Context,
            @StringRes title: Int,
            positiveAction: PromptDialogCallback,
            negativeAction: PromptDialogCallback = { _, _ -> },
            editText: EditText = EditText(ctx)
        ): AlertDialog {
            return createPromptDialog(ctx, ctx.getString(title), positiveAction, negativeAction, editText)
        }

        fun setDialogAttr(
            dialog: Dialog,
            isTransparent: Boolean = false,
            width: Int = WRAP_CONTENT,
            height: Int = WRAP_CONTENT,
            overlayWindow: Boolean = false
        ) {
            DialogUtil.setDialogAttr(dialog, isTransparent, width, height, overlayWindow)
        }
    }
}

typealias PromptDialogCallback = (dialog: DialogInterface, editText: EditText) -> Unit