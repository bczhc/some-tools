package pers.zhc.tools.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.widget.Toast;

public class ToastUtils {
    private static Toast toast;

    static {

    }

    public static void show(Context ctx, @StringRes int strRes) {
        if (ToastUtils.toast != null) {
            ToastUtils.toast.cancel();
        }
        ToastUtils.toast = Toast.makeText(ctx, strRes, Toast.LENGTH_SHORT);
        ToastUtils.toast.show();
    }

    public static void show(Context ctx, @NonNull CharSequence charSequence) {
        if (ToastUtils.toast != null) {
            ToastUtils.toast.cancel();
        }
        ToastUtils.toast = Toast.makeText(ctx, charSequence, Toast.LENGTH_SHORT);
        ToastUtils.toast.show();
    }
}
