package pers.zhc.tools.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.StringRes;
import androidx.core.content.FileProvider;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.BuildConfig;
import pers.zhc.tools.Info;
import pers.zhc.tools.R;

import java.io.File;
import java.util.Objects;

/**
 * @author bczhc
 */
public class Common {
    private static final String TAG = Common.class.getName();

    public static void showException(@NotNull Exception e, @NotNull Activity activity) {
        e.printStackTrace();
        activity.runOnUiThread(() -> ToastUtils.show(activity, e.toString()));
    }

    public static void showException(@NotNull Exception e, Context ctx) {
        ToastUtils.show(ctx, e.toString());
    }

    public static void showException(@NotNull Exception e, Context ctx, @NotNull Handler handler) {
        e.printStackTrace();
        handler.post(() -> ToastUtils.show(ctx, e.toString()));
    }

    @NotNull
    public static String getExternalStoragePath(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return Objects.requireNonNull(ctx.getExternalFilesDir(null)).toString();
    }

    /**
     * 安装apk
     *
     * @param context ctx
     * @param apk     本地apk
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

    @NotNull
    public static File getInternalDatabaseDir(@NotNull Context ctx) {
        final File dir = new File(ctx.getFilesDir().getPath() + File.separatorChar + "db");
        if (!dir.exists()) {
            Log.d(TAG, String.valueOf(dir.mkdirs()));
        }
        return dir;
    }

    @Contract("_, _ -> new")
    public static @NotNull File getInternalDatabaseDir(Context ctx, String name) {
        return new File(getInternalDatabaseDir(ctx), name);
    }

    @Contract("_, _ -> new")
    public static @NotNull File getInternalDatabaseFile(Context context, String name) {
        return new File(getInternalDatabaseDir(context), name);
    }

    @NotNull
    @Contract(pure = true)
    public static String getStaticResourceUrlString(String filename) {
        return Info.staticResourceRootURL + '/' + filename;
    }

    @Contract("false -> fail")
    public static void doAssertion(boolean condition) {
        if (!condition) throw new AssertionError("Assertion failed");
    }

    @NotNull
    public static String getAppMainExternalStoragePath(Context ctx) {
        return getExternalStoragePath(ctx) + File.separatorChar + "some-tools-app";
    }

    @NotNull
    @Contract("_ -> new")
    public static File getAppMainExternalStoragePathFile(Context ctx) {
        return new File(getExternalStoragePath(ctx), "some-tools-app");
    }

    public static void createAppMainExternalPath(Context ctx) {
        final File f = getAppMainExternalStoragePathFile(ctx);
        if (!f.exists()) {
            doAssertion(f.mkdirs());
        }
    }

    public static void runOnUiThread(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(r);
    }

    public static void toastTodo(Context context, String msg) {
        ToastUtils.show(context, msg);
    }

    public static void toastTodo(Context context, @StringRes int stringRes) {
        toastTodo(context, context.getString(stringRes));
    }

    public static void toastTodo(Context context) {
        toastTodo(context, R.string.function_not_implemented_toast);
    }
}
