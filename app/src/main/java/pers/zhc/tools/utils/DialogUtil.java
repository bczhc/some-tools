package pers.zhc.tools.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import pers.zhc.tools.R;

import java.util.Objects;

public class DialogUtil {
    public static void setDialogAttr(Dialog d, boolean isTransparent, int width, int height, boolean application_overlay) {
        Window window;
        try {
            window = Objects.requireNonNull(d.getWindow());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }
        if (application_overlay) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.setAttributes(new WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, 0, PixelFormat.RGB_888));
            } else                                 //noinspection deprecation
                window.setAttributes(new WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, 0, PixelFormat.RGB_888));
        }
        if (isTransparent) window.setBackgroundDrawableResource(R.color.transparent);
    }

    public static AlertDialog createConfirmationAD(Context ctx, DialogInterface.OnClickListener positiveAction, DialogInterface.OnClickListener negativeAction, int titleId, int width, int height, boolean application_overlay) {
        AlertDialog.Builder adb = new AlertDialog.Builder(ctx);
        AlertDialog ad = adb.setPositiveButton(R.string.confirm, positiveAction).setNegativeButton(R.string.cancel, negativeAction).setTitle(titleId).create();
        setDialogAttr(ad, false, width, height, application_overlay);
        ad.setCanceledOnTouchOutside(true);
        return ad;
    }

    public static void setADWithET_autoShowSoftKeyboard(EditText editText, Dialog ad) {
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.requestFocus();
        Objects.requireNonNull(ad.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
}
