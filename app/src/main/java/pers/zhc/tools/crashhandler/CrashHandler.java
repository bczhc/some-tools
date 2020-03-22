package pers.zhc.tools.crashhandler;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import pers.zhc.tools.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

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
        intent.putExtra("exception", stackTraceString);
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