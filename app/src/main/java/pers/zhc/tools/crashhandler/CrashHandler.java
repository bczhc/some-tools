package pers.zhc.tools.crashhandler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;

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
        final File crashDir = new File(Common.getExternalStoragePath(ctx), ctx.getString(R.string.some_tools_app)
                + File.separatorChar + ctx.getString(R.string.crash));
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
        final String stackTraceString = getExceptionStackTraceString(e);
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

    private String getExceptionStackTraceString(Throwable e) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(byteArrayOutputStream);
        e.printStackTrace(pw);
        Throwable cause = e.getCause();
        while (cause != null) {
            cause.printStackTrace(pw);
            cause = cause.getCause();
        }
        pw.flush();
        pw.close();
        return byteArrayOutputStream.toString();
    }
}