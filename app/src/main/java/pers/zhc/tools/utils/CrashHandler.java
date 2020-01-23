package pers.zhc.tools.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.u.common.FileMultipartUploader;

import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * UncaughtException处理类,当程序发生Uncaught异常的时候,有该类来接管程序,并记录发送错误报告.
 *
 * @author user
 */
public class CrashHandler implements UncaughtExceptionHandler {
    public static final String TAG = "CrashHandler";
    // CrashHandler实例
    @SuppressLint("StaticFieldLeak")
    private static CrashHandler INSTANCE = new CrashHandler();
    private CountDownLatch cdl = new CountDownLatch(2);
    // 系统默认的UncaughtException处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    // 程序的Context对象
    private Context mContext;
    // 用来存储设备信息和异常信息
    private Map<String, String> infos = new HashMap<>();

    // 用于格式化日期,作为日志文件名的一部分
    @SuppressLint("SimpleDateFormat")
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    /**
     * 保证只有一个CrashHandler实例
     */
    private CrashHandler() {
    }

    /**
     * 获取CrashHandler实例 ,单例模式
     */
    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化
     *
     * @param context c
     */
    public void init(Context context) {
        mContext = context;
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                cdl.await();
            } catch (InterruptedException ignored) {
            }
            exitProcess();
        }
    }

    private void exitProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex e
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        // 使用Toast来显示异常信息
        new Thread(() -> {
            Looper.prepare();
            ToastUtils.show(mContext, mContext.getString(R.string.crash_sorry_toast_information));
            cdl.countDown();
            Looper.loop();
        }).start();
        // 收集设备参数信息
        collectDeviceInfo(mContext);
        // 保存日志文件
        saveCrashInfo2File(ex);
        return true;
    }

    /**
     * 收集设备参数信息
     *
     * @param ctx c
     */
    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    versionCode = pi.getLongVersionCode() + "";
                } else //noinspection deprecation
                    versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "an error occurred when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object o = field.get(null);
                if (o != null) {
                    infos.put(field.getName(), o.toString());
                }
                Log.d(TAG, field.getName() + ": " + o);
            } catch (Exception e) {
                Log.e(TAG, "an error occurred when collect crash info", e);
            }
        }
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex e
     */
    private void saveCrashInfo2File(Throwable ex) {

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key).append("=").append(value).append("\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        System.out.println(result);
        sb.append(result);
        String infos = sb.toString();
        long timestamp = System.currentTimeMillis();
        String time = formatter.format(new Date());
        String fileName = "crash-" + time + "-" + timestamp + ".log";
        String path;
        path = Common.getExternalStoragePath(mContext) + File.separatorChar
                + mContext.getString(R.string.some_tools_app) + File.separatorChar + mContext.getString(R.string.crash);
        File file = new File(path + File.separatorChar + fileName);
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File dir = new File(path);
                if (!dir.exists()) {
                    System.out.println(dir.mkdirs());
                }
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(infos.getBytes());
                fos.close();
            }
            new Thread(() -> {
                //upload crash log to server
                //dialog
                Point point = new Point();
                ((Activity) mContext).getWindowManager().getDefaultDisplay().getSize(point);
                int width = point.x;
                int height = point.y;
                Looper.prepare();
                Dialog dialog = new Dialog(mContext);
                ScrollView sv = new ScrollView(mContext);
                TextView tv = new TextView(mContext);
                tv.setText(result);
                tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                sv.addView(tv);
                RelativeLayout bar = new RelativeLayout(mContext);
                bar.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                TextView barL = new TextView(mContext);
                TextView barR = new TextView(mContext);
                barR.setText(mContext.getString(R.string.please_choose));
                bar.setId(R.id.bar);
                barL.setText(R.string.crash_bar);
                barL.setTextSize(20);
                barL.setId(R.id.bar_l);
                barR.setTextSize(20);
                RelativeLayout.LayoutParams barR_lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                barR_lp.addRule(RelativeLayout.RIGHT_OF, R.id.bar_l);
                barR_lp.setMargins(10, 0, 0, 0);
                barR.setLayoutParams(barR_lp);
                RelativeLayout ll = new RelativeLayout(mContext);
                ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                bar.addView(barL);
                bar.addView(barR);
                ll.addView(bar);
                LinearLayout bottomButtonLL = new LinearLayout(mContext);
                bottomButtonLL.setId(R.id.ll_bottom);
                RelativeLayout.LayoutParams sv_rl_lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                sv_rl_lp.addRule(RelativeLayout.ABOVE, R.id.ll_bottom);
                sv_rl_lp.addRule(RelativeLayout.BELOW, R.id.bar);
                sv.setLayoutParams(sv_rl_lp);
                ll.addView(sv);
                bottomButtonLL.setOrientation(LinearLayout.HORIZONTAL);
                RelativeLayout.LayoutParams bb_lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bb_lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                bottomButtonLL.setLayoutParams(bb_lp);
                Button[] buttons = new Button[]{
                        new Button(mContext),
                        new Button(mContext)
                };
                dialog.setOnKeyListener((v, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK) this.cdl.countDown();
                    return true;
                });
                dialog.setCanceledOnTouchOutside(false);
                dialog.setCancelable(false);
                View.OnClickListener[] listeners = new View.OnClickListener[]{
                        v -> this.cdl.countDown(),
                        v -> {
                            barR.setTextColor(Color.BLUE);
                            barR.setText(R.string.uploading);
                            Handler handler = new Handler();
                            new Thread(() -> {
                                try {
                                    FileMultipartUploader.upload(BaseActivity.Infos.zhcUrlString + "/tools_app/crash_report.zhc", file);
                                    handler.post(() -> {
                                        barR.setTextColor(ContextCompat.getColor(mContext, R.color.done_green));
                                        barR.setText(R.string.upload_done);
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    handler.post(() -> {
                                        barR.setText(R.string.upload_failed);
                                        barR.setTextColor(Color.RED);
                                    });
                                }
                            }).start();
                        }
                };
                int[] strRes = new int[]{
                        R.string.exit,
                        R.string.upload_crash_report
                };
                for (int i = 0; i < buttons.length; i++) {
                    bottomButtonLL.addView(buttons[i]);
                    buttons[i].setOnClickListener(listeners[i]);
                    buttons[i].setText(strRes[i]);
                    buttons[i].setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1F));
                }
                ll.addView(bottomButtonLL);
                boolean permission = false;
                if (Build.VERSION.SDK_INT >= 23) {
                    permission = Settings.canDrawOverlays(mContext);
                }
                System.out.println("permission = " + permission);
                DialogUtil.setDialogAttr(dialog, false, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, permission);
                ll.setFocusable(true);
                ll.setFocusableInTouchMode(true);
                ll.setOnKeyListener((v, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK) this.cdl.countDown();
                    return true;
                });
                dialog.setContentView(ll, new ViewGroup.LayoutParams(((int) (((float) width) * .8)), ((int) (((float) height) * .8))));
                dialog.show();
                Looper.loop();
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}