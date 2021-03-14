package pers.zhc.tools.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.StringRes;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;

public class ToastUtils {
    private static Toast toast;

    public static void show(Context ctx, @StringRes int strRes) {
        show(ctx, ctx.getString(strRes));
    }

    /**
     * Toast on UI thread.
     *
     * @param ctx          context
     * @param charSequence string
     */
    public static void show(Context ctx, @NotNull CharSequence charSequence) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            if (toast != null) {
                toast.cancel();
            }
            toast = Toast.makeText(ctx, charSequence, Toast.LENGTH_SHORT);
            toast.show();
        });
    }

    public static void showError(Context ctx, @StringRes int error_msg_resId, Exception e) {
        ToastUtils.show(ctx, ctx.getString(R.string.concat, ctx.getString(error_msg_resId)
                , "\n" + e.toString()));
    }
}
