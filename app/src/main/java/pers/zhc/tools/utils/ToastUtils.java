package pers.zhc.tools.utils;

import android.app.Activity;
import android.content.Context;
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
     * Toast.
     *
     * @param ctx          context
     * @param charSequence string
     */
    public static void show(Context ctx, @NotNull CharSequence charSequence) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(ctx, charSequence, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Toast on UI Thread.
     *
     * @param activity     activity
     * @param charSequence string
     */
    public static void show(Activity activity, @NotNull CharSequence charSequence) {
        activity.runOnUiThread(() -> show((Context) activity, charSequence));
    }

    public static void showError(Context ctx, @StringRes int error_msg_resId, Exception e) {
        ToastUtils.show(ctx, ctx.getString(R.string.concat, ctx.getString(error_msg_resId)
                , "\n" + e.toString()));
    }
}
