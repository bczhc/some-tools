package pers.zhc.tools;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pers.zhc.tools.crashhandler.CrashHandler;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.ExternalJNI;
import pers.zhc.tools.utils.PermissionRequester;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.u.common.ReadIS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author bczhc
 * 代码首先是给人读的，只是恰好可以执行！
 * Machine does not care, but I care!
 */
public class BaseActivity extends Activity {
    public final App app = new App();
    protected final String TAG = this.getClass().getName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.e("Alert", "eee");
            ToastUtils.show(this, "eee");
        });
        app.addActivity(this);
        CrashHandler.install(this);
        ExternalJNI.ex(this);
        new PermissionRequester(() -> {
        }).requestPermission(this, Manifest.permission.INTERNET, RequestCode.REQUEST_PERMISSION_INTERNET);
        if (Infos.LAUNCHER_CLASS.equals(this.getClass())) {
            checkForUpdate(null);
        }
    }

    protected void checkForUpdate(@Nullable CheckForUpdateResultInterface checkForUpdateResultInterface) {
        new Thread(() -> {
            System.out.println("check update...");
            int myVersionCode = BuildConfig.VERSION_CODE;
            String myVersionName = BuildConfig.VERSION_NAME;
            try {
                String appURL = Infos.ZHC_STATIC_WEB_URL_STRING + "/res/app/" + getString(R.string.app_name) + "/debug";
                URL jsonURL = new URL(appURL + "/output.json");
                InputStream is = jsonURL.openStream();
                StringBuilder sb = new StringBuilder();
                new ReadIS(is).read(s -> sb.append(s).append("\n"));
                is.close();
                JSONArray jsonArray = new JSONArray(sb.toString());
                JSONObject jsonObject = jsonArray.getJSONObject(0).getJSONObject("apkData");
                int remoteVersionCode = jsonObject.getInt("versionCode");
                String remoteVersionName = jsonObject.getString("versionName");
                String remoteFileName = jsonObject.getString("outputFile");
                long fileSize = -1;
                final String length = "length";
                if (jsonObject.has(length)) {
                    fileSize = jsonObject.getLong(length);
                }
                boolean update = true;
                try {
                    String[] split = remoteVersionName.split("_");
                    long remoteBuildTime = Long.parseLong(split[1]);
                    String[] split1 = myVersionName.split("_");
                    long myBuildTime = Long.parseLong(split1[1]);
                    update = remoteBuildTime > myBuildTime;
                } catch (Exception ignored) {
                }
                if (checkForUpdateResultInterface != null) {
                    checkForUpdateResultInterface.onCheckForUpdateResult(update);
                }
                if (update) {
                    long finalFileSize = fileSize;
                    runOnUiThread(() -> {
                        AlertDialog.Builder adb = new AlertDialog.Builder(this);
                        TextView tv = new TextView(this);
                        tv.setText(getString(R.string.version_info, myVersionName, myVersionCode
                                , remoteVersionName, remoteVersionCode));
                        AlertDialog ad = adb.setTitle(R.string.found_update_whether_to_install)
                                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                                })
                                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                                    ExecutorService es = Executors.newCachedThreadPool();//使用线程池，目的是为了可以终止下载线程
                                    new PermissionRequester(() -> {
                                        Dialog downloadDialog = new Dialog(this);
                                        DialogUtil.setDialogAttr(downloadDialog, false
                                                , ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                                                , false);
                                        RelativeLayout rl = View.inflate(this, R.layout.progress_bar, null)
                                                .findViewById(R.id.rl);
                                        rl.setLayoutParams(new RelativeLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                        TextView progressTextView = rl.findViewById(R.id.progress_tv);
                                        TextView barTextView = rl.findViewById(R.id.progress_bar_title);
                                        barTextView.setText(R.string.downloading);
                                        ProgressBar pb = rl.findViewById(R.id.progress_bar);
                                        SeekBar seekBar = new SeekBar(this);
                                        seekBar.setLayoutParams(new ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                        progressTextView.setText(R.string.please_wait);
                                        downloadDialog.setContentView(rl);
                                        downloadDialog.setCanceledOnTouchOutside(false);
                                        downloadDialog.setCancelable(true);
                                        final OutputStream[] os = new OutputStream[1];
                                        final InputStream[] downloadInputStream = new InputStream[1];
                                        downloadDialog.setOnCancelListener(dialog1 -> {
                                            es.shutdownNow();
                                            Thread thread = new Thread(() -> {
                                                try {
                                                    os[0].close();
                                                    downloadInputStream[0].close();
                                                } catch (IOException | NullPointerException e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                            thread.start();
                                            try {
                                                thread.join();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        downloadDialog.show();
                                        es.execute(() -> {
                                            try {
                                                URL apkURL = new URL(appURL + "/" + remoteFileName);
                                                URLConnection urlConnection = apkURL.openConnection();
                                                downloadInputStream[0] = urlConnection.getInputStream();
                                                File apkDir = new File(Common.getExternalStoragePath(this)
                                                        + File.separatorChar + getString(R.string.some_tools_app), getString(R.string.apk));
                                                if (!apkDir.exists()) {
                                                    System.out.println("apkDir.mkdirs() = " + apkDir.mkdirs());
                                                }
                                                File apk = new File(apkDir, remoteFileName);
                                                os[0] = new FileOutputStream(apk, false);
                                                int readLen;
                                                long read = 0L;
                                                byte[] buffer = new byte[1024];
                                                while ((readLen = downloadInputStream[0].read(buffer)) != -1) {
                                                    os[0].write(buffer, 0, readLen);
                                                    read += readLen;
                                                    long finalRead = read;
                                                    runOnUiThread(() -> {
                                                        progressTextView.setText(getString(R.string.download_progress
                                                                , finalRead, finalFileSize));
                                                        pb.setProgress((int) (((double) finalRead) / ((double) finalFileSize) * 100D));
                                                    });
                                                }
                                                runOnUiThread(() -> Common.installApk(this, apk));
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            } finally {
                                                downloadDialog.dismiss();
                                            }
                                        });
                                        es.shutdown();
                                    }).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
                                })
                                .setView(tv).create();
                        DialogUtil.setDialogAttr(ad, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                                , false);
                        ad.setCanceledOnTouchOutside(false);
                        ad.show();
                    });
                }
            } catch (IOException e) {
                Common.showException(e, this);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        app.pop();
        super.onDestroy();
    }


    /**
     * onConfigurationChanged
     * the package:android.content.res.Configuration.
     *
     * @param newConfig, The new device configuration.
     *                   当设备配置信息有改动（比如屏幕方向的改变，实体键盘的推开或合上等）时，
     *                   并且如果此时有activity正在运行，系统会调用这个函数。
     *                   注意：onConfigurationChanged只会监测应用程序在AndroidManifest.xml中通过
     *                   android:configChanges="xxxx"指定的配置类型的改动；
     *                   而对于其他配置的更改，则系统会onDestroy()当前Activity，然后重启一个新的Activity实例。
     */
    @Override
    public void onConfigurationChanged(@SuppressWarnings("NullableProblems") Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        String TAG = "onConfigurationChanged";
        // 检测屏幕的方向：纵向或横向
        if (this.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            //当前为横屏， 在此处添加额外的处理代码
            Log.d(TAG, "onConfigurationChanged: 当前为横屏");
        } else if (this.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            //当前为竖屏， 在此处添加额外的处理代码
            Log.d(TAG, "onConfigurationChanged: 当前为竖屏");
        }
        //检测实体键盘的状态：推出或者合上
        if (newConfig.hardKeyboardHidden
                == Configuration.HARDKEYBOARDHIDDEN_NO) {
            //实体键盘处于推出状态，在此处添加额外的处理代码
            Log.d(TAG, "onConfigurationChanged: 实体键盘处于推出状态");
        } else if (newConfig.hardKeyboardHidden
                == Configuration.HARDKEYBOARDHIDDEN_YES) {
            //实体键盘处于合上状态，在此处添加额外的处理代码
            Log.d(TAG, "onConfigurationChanged: 实体键盘处于合上状态");
        }
    }

    protected byte ckV() {
        try {
            URLConnection urlConnection = new URL(Infos.ZHC_URL_STRING + "/i.zhc?t=tools_v").openConnection();
            InputStream is = urlConnection.getInputStream();
            byte[] b = new byte[urlConnection.getContentLength()];
            System.out.println("is.read(b) = " + is.read(b));
            return b[0];
        } catch (IOException ignored) {

        }
        return 1;
    }

    protected interface CheckForUpdateResultInterface {
        /**
         * callback
         *
         * @param update b
         */
        void onCheckForUpdateResult(boolean update);
    }

    public static class Infos {
        public static final String ZHC_URL_STRING = "http://bczhc.free.idcfengye.com";
        public static final String ZHC_STATIC_WEB_URL_STRING = "http://bczhc.gitee.io/web";
        public static final Class<?> LAUNCHER_CLASS = MainActivity.class;
    }

    public static class RequestCode {
        public static final int START_ACTIVITY_0 = 0;
        public static final int START_ACTIVITY_1 = 1;
        public static final int START_ACTIVITY_2 = 2;
        public static final int START_ACTIVITY_3 = 3;
        @SuppressWarnings("unused")
        public static final int START_ACTIVITY_4 = 4;
        public static final int REQUEST_PERMISSION_INTERNET = 5;
        public static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 6;
        public static final int REQUEST_CAPTURE_SCREEN = 7;
        public static final int REQUEST_OVERLAY = 8;
    }

    public static class BroadcastIntent {
        public static final String START_FLOATING_BOARD = "pers.zhc.tools.START_FB";
    }

    public static class App {
        private final Stack<Activity> activities;

        App() {
            activities = new Stack<>();
        }

        void addActivity(Activity activity) {
            if (!activities.contains(activity)) {
                activities.push(activity);
            }
        }

        void pop() {
            activities.pop();
        }

        public void finishAllActivities() {
            for (Activity activity : activities) {
                if (activity != null) {
                    activity.finish();
                }
            }
        }
    }
}
