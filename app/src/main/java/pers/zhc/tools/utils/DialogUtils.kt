package pers.zhc.tools.utils

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.tools.R

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
            message: String? = null,
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
            ).apply {
                message?.let { setMessage(it) }
            }
        }

        fun createConfirmationAlertDialog(
            ctx: Context,
            positiveAction: DialogInterface.OnClickListener,
            negativeAction: DialogInterface.OnClickListener = DialogInterface.OnClickListener { _, _ -> },
            view: View? = null,
            @StringRes titleRes: Int,
            message: String? = null,
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
            ).apply {
                message?.let { setMessage(message) }
            }
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

fun MaterialAlertDialogBuilder.setPositiveAction(action: ((dialog: DialogInterface, which: Int) -> Unit)?): MaterialAlertDialogBuilder {
    return this.setPositiveButton(R.string.confirm, action)
}

fun MaterialAlertDialogBuilder.defaultNegativeButton(): MaterialAlertDialogBuilder {
    return this.setPositiveAction(null)
}

fun MaterialAlertDialogBuilder.setNegativeAction(action: ((dialog: DialogInterface, which: Int) -> Unit)?): MaterialAlertDialogBuilder {
    return this.setNegativeButton(R.string.cancel_btn, action)
}

fun MaterialAlertDialogBuilder.setNegativeAction(): MaterialAlertDialogBuilder {
    return this.setNegativeAction(null)
}

typealias PromptDialogCallback = (dialog: DialogInterface, editText: EditText) -> Unit