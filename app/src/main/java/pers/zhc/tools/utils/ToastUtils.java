package pers.zhc.tools.utils;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import pers.zhc.tools.R;

public class ToastUtils {
    private static Toast toast;

    public static void show(Context ctx, @StringRes int strRes) {
        if (ToastUtils.toast != null) {
            ToastUtils.toast.cancel();
        }
        ToastUtils.toast = Toast.makeText(ctx, strRes, Toast.LENGTH_SHORT);
        ToastUtils.toast.show();
    }

    public static void show(Context ctx, @NonNull CharSequence charSequence) {
        ((AppCompatActivity) ctx).runOnUiThread(() -> {
            if (ToastUtils.toast != null) {
                ToastUtils.toast.cancel();
            }
            ToastUtils.toast = Toast.makeText(ctx, charSequence, Toast.LENGTH_SHORT);
            ToastUtils.toast.show();
        });
    }

    public static void showError(Context ctx, @StringRes int error_msg_resId, Exception e) {
        ToastUtils.show(ctx, ctx.getString(R.string.concat, ctx.getString(error_msg_resId)
                , "\n" + e.toString()));
    }
}
