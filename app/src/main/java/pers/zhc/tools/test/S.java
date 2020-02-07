package pers.zhc.tools.test;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.floatingdrawing.JNI;

import java.util.Arrays;

public class S extends BaseActivity {
    @SuppressLint("Range")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JNI jni = new JNI();
        byte[] b = new byte[6];
        jni.intToByteArray(b, 2333, 1);
        System.out.println(Arrays.toString(b));
        int i = jni.byteArrayToInt(b, 1);
        System.out.println("i = " + i);
    }
}
