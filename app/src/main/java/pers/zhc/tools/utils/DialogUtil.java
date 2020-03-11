package pers.zhc.tools.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.os.Build;
import android.support.annotation.Nullable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import pers.zhc.tools.R;
import pers.zhc.tools.floatingdrawing.FloatingDrawingBoardMainActivity;

import java.util.Objects;

public class DialogUtil {
    /**
     *
     * @param d d
     * @param isTransparent b
     * @param width w
     * @param height h
     * @param application_overlay overlay. null is auto
     */
    public static void setDialogAttr(Dialog d, boolean isTransparent, int width, int height, @Nullable Boolean application_overlay) {
        final boolean[] overlay = {false};
        if (application_overlay == null) {
            new FloatingDrawingBoardMainActivity.CheckOverlayPermission(((Activity) d.getContext())) {
                @Override
                public void granted() {
                    overlay[0] = true;
                }

                @Override
                public void denied() {
                }

            };
        } else overlay[0] = application_overlay;
        Window window;
        try {
            window = Objects.requireNonNull(d.getWindow());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }
        if (overlay[0]) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.setAttributes(new WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, 0, PixelFormat.RGB_888));
            } else                                 //noinspection deprecation
                window.setAttributes(new WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_SYSTEM_ERROR, 0, PixelFormat.RGB_888));
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
