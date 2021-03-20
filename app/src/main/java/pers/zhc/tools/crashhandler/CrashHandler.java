package pers.zhc.tools.crashhandler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author bczhc
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private final Context ctx;

    private CrashHandler(Context context) {
        this.ctx = context;
    }

    public static void install(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
    }

    static void save2File(Context ctx, String filename, String content) {
        final File crashDir = new File(Common.getAppMainExternalStoragePath(ctx) + File.separatorChar + "crash");
        if (!crashDir.exists()) {
            Log.d(CrashHandler.class.getName(), "save2File: " + crashDir.mkdirs());
        }
        final File file = new File(crashDir, filename);
        System.out.println("crashDir.getPath() = " + crashDir.getPath());
        System.out.println("file.getPath() = " + file.getPath());
        OutputStream os = null;
        try {
            os = new FileOutputStream(file, false);
            //noinspection CharsetObjectCanBeUsed
            os.write(content.getBytes("UTF-8"));
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        final Intent intent = new Intent();
        final String stackTraceString = getExceptionStackTraceString(t, e);
        intent.setClass(ctx, CrashReportActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final long currentTimeMillis = System.currentTimeMillis();
        final Date date = new Date(currentTimeMillis);
        @SuppressLint("SimpleDateFormat") final String dateString = new SimpleDateFormat("yy-MM-dd-HH-mm-ss").format(date);
        String filename = "crash-" + dateString + "-" + currentTimeMillis + ".txt";
        intent.putExtra("exception", stackTraceString);
        intent.putExtra("filename", filename);
        ctx.startActivity(intent);
        try {
            ((BaseActivity) ctx).app.finishAllActivities();
        } catch (Exception ignored) {
        }
        System.exit(1);
    }

    private String getExceptionStackTraceString(Thread t, Throwable e) {
        StringBuilder sb = new StringBuilder();
        getExceptionStackTraceString(sb, t, e);
        return sb.toString();
    }

    private void getExceptionStackTraceString(@NotNull StringBuilder sb, @NotNull Thread t, @NotNull Throwable e) {
        StackTraceElement[] ses = e.getStackTrace();
        sb.append("Exception in thread \"").append(t.getName()).append("\" ").append(e.toString()).append('\n');
        for (StackTraceElement se : ses) {
            sb.append("\tat ").append(se).append('\n');
        }
        Throwable ec = e.getCause();
        if (ec != null) {
            getExceptionStackTraceString(sb, t, ec);
        }
    }
}