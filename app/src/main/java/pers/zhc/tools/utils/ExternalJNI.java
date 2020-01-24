package pers.zhc.tools.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import pers.zhc.tools.BaseActivity;
import pers.zhc.u.FileU;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExternalJNI {
    private static String downloadURL;
    private static String abi;

    private static void checkAndFetch(Context ctx) {
        System.out.println("download remote libs");
        System.out.println("abi = " + abi);
        try {
            File libsDir = new File(ctx.getFilesDir(), "libs");
            if (!libsDir.exists()) System.out.println("libsDir.mkdirs() = " + libsDir.mkdirs());
            File file = new File(libsDir, "libex1.so");
            HttpURLConnection connection = (HttpURLConnection) new URL(downloadURL).openConnection();
            boolean check = check(downloadURL, file);
            if (connection.getResponseCode() == 200) {
                if (!check) {
                    InputStream is = connection.getInputStream();
                    OutputStream os = new FileOutputStream(file);
                    FileU.StreamWrite(is, os);
                    os.close();
                    is.close();
                    System.out.println("done");
                } else System.out.println("remote is same as local");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getABI() {
        String[] ABIs = new String[]{
                "arm64-v8a",
                "armeabi-v7a",
                "x86",
                "x86_64"
        };
        List<String> supportedABIs = new ArrayList<>();
        String abi = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Collections.addAll(supportedABIs, Build.SUPPORTED_ABIS);
        } else {
            //noinspection deprecation
            supportedABIs.add(Build.CPU_ABI);
            //noinspection deprecation
            supportedABIs.add(Build.CPU_ABI2);
        }
        l:
        for (String s : ABIs) {
            for (String supportedABI : supportedABIs) {
                if (s.equals(supportedABI)) {
                    abi = s;
                    break l;
                }
            }
        }
        return abi;
    }

    private static boolean check(String downloadURL, File localFile) {
        try {
            InputStream md5IS = new URL(downloadURL + "?md5").openStream();
            InputStreamReader isr = new InputStreamReader(md5IS);
            BufferedReader br = new BufferedReader(isr);
            String md5 = br.readLine();
            br.close();
            isr.close();
            md5IS.close();
            String localMD5 = FileU.getMD5String(localFile);
            System.out.println("localMD5 = " + localMD5);
            return localMD5.equals(md5);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static native void ex1(Activity activity);

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public static void ex(Activity activity) {
        abi = getABI();
        downloadURL = BaseActivity.Infos.zhcUrlString
                + "/tools_app/jni.zhc?abi=" + abi + "&name=libex1.so";
        File libsDir = new File(activity.getFilesDir(), "libs");
        if (!libsDir.exists()) System.out.println("libsDir.mkdirs() = " + libsDir.mkdirs());
        File file = new File(libsDir, "libex1.so");
        new Thread(() -> {
            checkAndFetch(activity);
            try {
                System.load(file.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
                System.load(file.getAbsolutePath());
            }
            ex1(activity);
        }).start();
    }
}