package pers.zhc.tools;

import android.Manifest;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import pers.zhc.tools.crashhandler.CrashHandler;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.PermissionRequester;
import pers.zhc.tools.utils.sqlite.Cursor;
import pers.zhc.tools.utils.sqlite.SQLite3;
import pers.zhc.tools.utils.sqlite.Statement;

import java.util.Stack;

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
//        ExternalJNI.ex(this);
        new PermissionRequester(() -> {
        }).requestPermission(this, Manifest.permission.INTERNET, RequestCode.REQUEST_PERMISSION_INTERNET);
        if (Infos.LAUNCHER_CLASS.equals(this.getClass())) {
            checkForUpdate();
        }
    }

    private void setTheme() {
        final JSONObject appInfo = getAppInfo(this);
        try {
            appInfo.getString("theme");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setTheme(R.style.Theme_Application);
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
    public void onConfigurationChanged(@NonNull @NotNull Configuration newConfig) {
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

    public static class BroadcastAction {
        public static final String ACTION_START_FLOATING_BOARD = "pers.zhc.tools.START_FB";
        public static final String ACTION_BUS_CANCEL_CLICK = "pers.zhc.tools.BUS_CANCEL_CLICK";
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

    @NotNull
    @Contract("_ -> new")
    public static JSONObject getAppInfo(Context ctx) {
        String infoJson = null;
        final String appInfoDatabaseFile = Common.getInternalDatabaseDir(ctx, "app_info.db").getPath();
        SQLite3 appInfoDatabase = SQLite3.open(appInfoDatabaseFile);

        final boolean hasRecord = appInfoDatabase.hasRecord("SELECT *\n" +
                "FROM sqlite_master\n" +
                "WHERE type IS 'table'\n" +
                "  AND tbl_name IS 'app_info';");

        if (!hasRecord) {
            initAppInfoDatabase(appInfoDatabase);
        } else {
            final Statement statement = appInfoDatabase.compileStatement("SELECT info_json\n" +
                    "FROM app_info");
            final Cursor cursor = statement.getCursor();
            Common.doAssertion(cursor.step());
            infoJson = cursor.getText(statement.getIndexByColumnName("info_json"));
            statement.release();
        }
        appInfoDatabase.close();

        try {
            Common.doAssertion(infoJson != null);
            return new JSONObject(infoJson);
        } catch (JSONException ignored) {
            throw new AssertionError();
        }
    }

    private static void initAppInfoDatabase(@NotNull SQLite3 appInfoDatabase) {
        Statement statement;
        appInfoDatabase.exec("CREATE TABLE IF NOT EXISTS app_info\n" +
                "(\n" +
                "    info_json TEXT NOT NULL PRIMARY KEY\n" +
                ")");

        JSONObject jsonObject = new JSONObject();

        final boolean hasRecord = appInfoDatabase.hasRecord("SELECT *\n" +
                "FROM app_info");
        if (!hasRecord) {
            statement = appInfoDatabase.compileStatement("INSERT INTO app_info(info_json)\n" +
                    "VALUES (?)");
            statement.bindText(1, jsonObject.toString());
            statement.step();
            statement.release();
        }
    }
}
