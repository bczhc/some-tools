package pers.zhc.tools.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pers.zhc.tools.Infos;

public class ExternalJNI {
    private static String downloadURL;
    private static String abi;

    private static void fetch(Context ctx) throws IOException {
        System.out.println("download remote libs");
        System.out.println("abi = " + abi);
        File libsDir = new File(ctx.getFilesDir(), "libs");
        if (!libsDir.exists()) {
            System.out.println("libsDir.mkdirs() = " + libsDir.mkdirs());
        }
        File file = new File(libsDir, "libex1.so");
        HttpURLConnection connection = (HttpURLConnection) new URL(downloadURL).openConnection();
        if (connection.getResponseCode() == 200) {
            InputStream is = connection.getInputStream();
            OutputStream os = new FileOutputStream(file, false);
            IOUtilsKt.writeTo(is, os);
            os.close();
            is.close();
            System.out.println("done");
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
            InputStream md5IS = new URL(downloadURL + "&md5").openStream();
            InputStreamReader isr = new InputStreamReader(md5IS);
            BufferedReader br = new BufferedReader(isr);
            String md5 = br.readLine();
            br.close();
            isr.close();
            md5IS.close();
            String localMD5 = DigestUtil.getFileDigestString(localFile, "MD5");
            System.out.println("localMD5 = " + localMD5);
            return localMD5.equals(md5);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static native void ex1(AppCompatActivity activity);

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public static void ex(AppCompatActivity activity) {
        Handler handler = new Handler();
        new Thread(() -> {
            abi = getABI();
            downloadURL = Infos.ZHC_URL_STRING
                    + "/tools_app/jni.zhc?abi=" + abi + "&name=libex1.so";
            File libsDir = new File(activity.getFilesDir(), "libs");
            if (!libsDir.exists()) {
                System.out.println("libsDir.mkdirs() = " + libsDir.mkdirs());
            }
            File file = new File(libsDir, "libex1.so");
            boolean check;
            if (!file.exists()) {
                check = false;
            } else {
                check = check(downloadURL, file);
            }
            try {
                if (!check) {
                    fetch(activity);
                }
                if (file.exists()) {
                    try {
                        System.load(file.getCanonicalPath());
                        System.out.println("load remote lib");
                        handler.post(() -> {
                            try {
                                ex1(activity);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.load(file.getAbsolutePath());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}