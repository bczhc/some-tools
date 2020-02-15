package pers.zhc.tools.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import pers.zhc.tools.BuildConfig;

import java.io.File;
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

    /**
     * 安装apk
     *
     * @param context  ctx
     * @param apk 本地apk
     */
    public static void installApk(Context context, File apk) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", apk);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            Uri uri = Uri.fromFile(apk);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        }
        context.startActivity(intent);
    }
}