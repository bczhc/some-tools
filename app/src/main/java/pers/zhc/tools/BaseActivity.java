package pers.zhc.tools;

import android.Manifest;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.Stack;

import pers.zhc.tools.crashhandler.CrashHandler;
import pers.zhc.tools.utils.ExternalJNI;
import pers.zhc.tools.utils.PermissionRequester;

/**
 * @author bczhc
 * <p>代码首先是给人读的，只是恰好可以执行！</p>
 * <p>Machine does not care, but I care!</p>
 * <p>I love reinventing whells!</p>
 */
public class BaseActivity extends AppCompatActivity {
    public final App app = new App();
    protected final String TAG = this.getClass().getName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app.addActivity(this);
        CrashHandler.install(this);
        ExternalJNI.ex(this);
        new PermissionRequester(() -> {
        }).requestPermission(this, Manifest.permission.INTERNET, RequestCode.REQUEST_PERMISSION_INTERNET);
        if (Infos.LAUNCHER_CLASS.equals(this.getClass())) {
            checkForUpdate();
        }
    }

    protected void checkForUpdate() {
    }

    @Override
    protected void onDestroy() {
        app.pop();
        super.onDestroy();
    }


    /**
     * onConfigurationChanged
     *
     * @param newConfig, The new device configuration.
     *                   当设备配置信息有改动（比如屏幕方向的改变，实体键盘的推开或合上等）时，
     *                   并且如果此时有activity正在运行，系统会调用这个函数。
     *                   注意：onConfigurationChanged只会监测应用程序在AndroidManifest.xml中通过
     *                   android:configChanges="xxxx"指定的配置类型的改动；
     *                   而对于其他配置的更改，则系统会onDestroy()当前Activity，然后重启一个新的Activity实例。
     */
    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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

    protected interface CheckForUpdateResultInterface {
        /**
         * callback
         *
         * @param update b
         */
        void onCheckForUpdateResult(boolean update);
    }

    public static class RequestCode {
        public static final int START_ACTIVITY_0 = 0;
        public static final int START_ACTIVITY_1 = 1;
        public static final int START_ACTIVITY_2 = 2;
        public static final int START_ACTIVITY_3 = 3;
        public static final int START_ACTIVITY_4 = 4;
        public static final int REQUEST_PERMISSION_INTERNET = 5;
        public static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 6;
        public static final int REQUEST_CAPTURE_SCREEN = 7;
        public static final int REQUEST_OVERLAY = 8;
        public static final int REQUEST_USB_PERMISSION = 9;
    }

    public static class BroadcastIntent {
        public static final String START_FLOATING_BOARD = "pers.zhc.tools.START_FB";
    }

    public static class App {
        private final Stack<AppCompatActivity> activities;

        App() {
            activities = new Stack<>();
        }

        void addActivity(AppCompatActivity activity) {
            if (!activities.contains(activity)) {
                activities.push(activity);
            }
        }

        void pop() {
            activities.pop();
        }

        public void finishAllActivities() {
            for (AppCompatActivity activity : activities) {
                if (activity != null) {
                    activity.finish();
                }
            }
        }
    }
}
