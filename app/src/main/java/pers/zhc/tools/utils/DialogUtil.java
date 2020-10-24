package pers.zhc.tools.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import pers.zhc.tools.R;

import java.util.Objects;

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

    public static AlertDialog createConfirmationAlertDialog(Context ctx, DialogInterface.OnClickListener positiveAction, DialogInterface.OnClickListener negativeAction, int titleId, int width, int height, boolean applicationOverlay) {
        AlertDialog.Builder adb = new AlertDialog.Builder(ctx);
        AlertDialog ad = adb.setPositiveButton(R.string.confirm, positiveAction).setNegativeButton(R.string.cancel, negativeAction).setTitle(titleId).create();
        setDialogAttr(ad, false, width, height, applicationOverlay);
        ad.setCanceledOnTouchOutside(true);
        return ad;
    }

    public static void setAlertDialogWithEditTextAndAutoShowSoftKeyBoard(EditText editText, Dialog ad) {
        InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        ad.setOnShowListener(dialog -> imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0));
        ad.setOnDismissListener(dialog -> imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0));
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
    }

    public static AlertDialog createPromptDialog(Context ctx, @StringRes int strId, PromptDialogCallback callback, DialogInterface.OnClickListener negativeAction){
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

    public static AlertDialog createPromptDialog(Context ctx, @StringRes int strId, PromptDialogCallback callback){
        return createPromptDialog(ctx, strId, callback, (dialog, which) -> {});
    }
}
