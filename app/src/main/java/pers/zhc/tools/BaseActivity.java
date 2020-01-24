package pers.zhc.tools;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import pers.zhc.tools.utils.CrashHandler;
import pers.zhc.tools.utils.ExternalJNI;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class BaseActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
        ExternalJNI.ex(this);
    }

    File getVFile(Context ctx) {
        File filesDir = ctx.getFilesDir();
        if (!filesDir.exists()) System.out.println("filesDir.mkdirs() = " + filesDir.mkdirs());
        File vF = new File(filesDir.toString() + "/v");
        if (!vF.exists()) {
            try {
                System.out.println("vF.createNewFile() = " + vF.createNewFile());
                OutputStream os = new FileOutputStream(vF);
                OutputStreamWriter osw = new OutputStreamWriter(os, "GBK");
                BufferedWriter bw = new BufferedWriter(osw);
//                bw.write("MTkwODI1-jzsvGT4h1g==");
                bw.write("OTk5OTk5-n1NbWfU1tQ==");
                bw.flush();
                bw.close();
                osw.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return vF;
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
            URLConnection urlConnection = new URL(Infos.zhcUrlString + "/i.zhc?t=tools_v").openConnection();
            InputStream is = urlConnection.getInputStream();
            byte[] b = new byte[urlConnection.getContentLength()];
            System.out.println("is.read(b) = " + is.read(b));
            return b[0];
        } catch (IOException ignored) {

        }
        return 1;
    }

    public static class Infos {
        public static String zhcUrlString = "http://bczhc.free.idcfengye.com";
    }

}
