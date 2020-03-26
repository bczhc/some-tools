package pers.zhc.tools.crashhandler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import pers.zhc.tools.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author bczhc
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Context ctx;

    private CrashHandler(Context context) {
        this.ctx = context;
    }

    public static void install(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
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