package pers.zhc.tools.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import pers.zhc.tools.BuildConfig;

import java.util.Objects;

public class Common {
    public static void showException(Exception e, Activity activity) {
        e.printStackTrace();
        activity.runOnUiThread(() -> ToastUtils.show(activity, e.toString()));
    }

    public static String getExternalStoragePath(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //noinspection deprecation
            return Environment.getExternalStorageDirectory().toString();
        }
        return Objects.requireNonNull(ctx.getExternalFilesDir(null)).toString();
    }
}