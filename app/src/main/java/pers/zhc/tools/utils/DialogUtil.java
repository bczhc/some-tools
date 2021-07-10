package pers.zhc.tools.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;

import java.util.Objects;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * @author bczhc
 */
public class DialogUtil {
    /**
     * @param d                  d
     * @param isTransparent      b
     * @param width              w
     * @param height             h
     * @param applicationOverlay overlay. null is auto
     */
    public static void setDialogAttr(Dialog d, boolean isTransparent, int width, int height, @Nullable Boolean applicationOverlay) {
        boolean overlay = false;
        if (applicationOverlay == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(d.getContext())) {
                    overlay = true;
                }
            }
        } else {
            overlay = applicationOverlay;
        }
        Window window;
        try {
            window = Objects.requireNonNull(d.getWindow());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }
        if (overlay) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.setAttributes(new WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, 0, PixelFormat.RGB_888));
            } else {
                //noinspection deprecation
                window.setAttributes(new WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_SYSTEM_ERROR, 0, PixelFormat.RGB_888));
            }
        }
        if (isTransparent) {
            window.setBackgroundDrawableResource(R.color.transparent);
        }
    }

    public static void setDialogAttr(Dialog d, boolean isTransparent, @Nullable Boolean applicationOverlay) {
        setDialogAttr(d, isTransparent, WRAP_CONTENT, WRAP_CONTENT, applicationOverlay);
    }

        @NotNull
    public static AlertDialog createConfirmationAlertDialog(Context ctx, DialogInterface.OnClickListener positiveAction, DialogInterface.OnClickListener negativeAction, int titleId, int width, int height, boolean applicationOverlay) {
        return createConfirmationAlertDialog(ctx, positiveAction, negativeAction, null, titleId, width, height, applicationOverlay);
    }

    @NotNull
    public static AlertDialog createConfirmationAlertDialog(Context ctx, DialogInterface.OnClickListener positiveAction, DialogInterface.OnClickListener negativeAction, String title, int width, int height, boolean applicationOverlay) {
        return createConfirmationAlertDialog(ctx, positiveAction, negativeAction, null, title, width, height, applicationOverlay);
    }

    @NotNull
    public static AlertDialog createConfirmationAlertDialog(Context ctx, DialogInterface.OnClickListener positiveAction, @Nullable DialogInterface.OnClickListener negativeAction, @Nullable View view, String title, int width, int height, boolean applicationOverlay) {
        final TextView titleTV = new TextView(ctx);
        titleTV.setTextSize(20);
        titleTV.setText(title);
        AlertDialog.Builder adb = new AlertDialog.Builder(ctx, R.style.Theme_Application_DayNight_Dialog_Alert);
        adb.setPositiveButton(R.string.confirm, positiveAction).setNegativeButton(R.string.cancel, negativeAction == null ? (dialog, which) -> {
        } : negativeAction).setCustomTitle(titleTV);
        if (view != null) {
            adb.setView(view);
        }
        AlertDialog ad = adb.create();
        setDialogAttr(ad, false, width, height, applicationOverlay);
        ad.setCanceledOnTouchOutside(true);
        return ad;
    }

    @NotNull
    public static AlertDialog createConfirmationAlertDialog(Context ctx, DialogInterface.OnClickListener positiveAction, @Nullable DialogInterface.OnClickListener negativeAction, @Nullable View view, int titleId, int width, int height, boolean applicationOverlay) {
        return createConfirmationAlertDialog(ctx, positiveAction, negativeAction, view, ctx.getString(titleId), width, height, applicationOverlay);
    }

    @NotNull
    public static AlertDialog createConfirmationAlertDialog(Context ctx, DialogInterface.OnClickListener positiveAction, int titleId) {
        return createConfirmationAlertDialog(ctx, positiveAction, (dialog, which) -> {
        }, titleId, WRAP_CONTENT, WRAP_CONTENT, false);
    }

    @NotNull
    public static AlertDialog createConfirmationAlertDialog(Context ctx, DialogInterface.OnClickListener positiveAction, int titleId, boolean applicationOverlay) {
        return createConfirmationAlertDialog(ctx, positiveAction, (dialog, which) -> {
        }, titleId, WRAP_CONTENT, WRAP_CONTENT, applicationOverlay);
    }

    @NotNull
    public static AlertDialog createConfirmationAlertDialog(Context ctx, DialogInterface.OnClickListener positiveAction, String title) {
        return createConfirmationAlertDialog(ctx, positiveAction, (dialog, which) -> {
        }, title, WRAP_CONTENT, WRAP_CONTENT, false);
    }

    @NotNull
    public static AlertDialog createConfirmationAlertDialog(Context ctx, DialogInterface.OnClickListener positiveAction, DialogInterface.OnClickListener negativeAction, int titleId) {
        return createConfirmationAlertDialog(ctx, positiveAction, negativeAction, titleId, WRAP_CONTENT, WRAP_CONTENT, false);
    }

    public static void setAlertDialogWithEditTextAndAutoShowSoftKeyBoard(@NotNull EditText editText, @NotNull Dialog ad) {
        InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        ad.setOnShowListener(dialog -> imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0));
        ad.setOnDismissListener(dialog -> imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0));
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
    }

    public static AlertDialog createPromptDialog(Context ctx, @StringRes int strId, PromptDialogCallback callback, DialogInterface.OnClickListener negativeAction) {
        AlertDialog[] ad = new AlertDialog[1];
        AlertDialog.Builder adb = new AlertDialog.Builder(ctx);
        EditText et = new EditText(ctx);
        adb.setTitle(strId)
                .setView(et)
                .setPositiveButton(R.string.confirm, (dialog, which) -> callback.confirm(et, ad[0]))
                .setNegativeButton(R.string.cancel, negativeAction);
        ad[0] = adb.create();
        return ad[0];
    }

    public static AlertDialog createPromptDialog(Context ctx, @StringRes int strId, PromptDialogCallback callback) {
        return createPromptDialog(ctx, strId, callback, (dialog, which) -> {
        });
    }

    @NotNull
    public static AlertDialog createAlertDialogWithNeutralButton(Context ctx,
                                                                 DialogInterface.OnClickListener positiveAction,
                                                                 DialogInterface.OnClickListener negativeAction,
                                                                 @StringRes int titleStrRes) {
        return createAlertDialogWithNeutralButton(ctx, positiveAction, negativeAction, (dialog, which) -> {
        }, titleStrRes);
    }

    @NotNull
    public static AlertDialog createAlertDialogWithNeutralButton(Context ctx,
                                                                 DialogInterface.OnClickListener positiveAction,
                                                                 DialogInterface.OnClickListener negativeAction,
                                                                 DialogInterface.OnClickListener neutralButtonAction,
                                                                 @StringRes int titleStrRes) {
        return createAlertDialogWithNeutralButton(ctx, R.string.yes, positiveAction, R.string.no, negativeAction, R.string.cancel, neutralButtonAction, titleStrRes);
    }

    @NotNull
    public static AlertDialog createAlertDialogWithNeutralButton(Context ctx,
                                                                 @StringRes int positiveButtonText,
                                                                 DialogInterface.OnClickListener positiveAction,
                                                                 @StringRes int negativeButtonText,
                                                                 DialogInterface.OnClickListener negativeAction,
                                                                 @StringRes int neutralButtonText,
                                                                 DialogInterface.OnClickListener neutralButtonAction,
                                                                 @StringRes int titleStrRes) {
        AlertDialog.Builder adb = new AlertDialog.Builder(ctx);
        adb.setPositiveButton(positiveButtonText, positiveAction)
                .setNegativeButton(negativeButtonText, negativeAction)
                .setNeutralButton(neutralButtonText, neutralButtonAction)
                .setTitle(titleStrRes);
        return adb.create();
    }
}
