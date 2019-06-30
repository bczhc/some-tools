package com.zhc.codec;

import android.widget.TextView;

public class JNI {
    private TextView tv;

    JNI(TextView tv) {
        this.tv = tv;
    }

    static {
        System.loadLibrary("doJNI");
    }

    native int qmcDecode(String f, String dF);

    @SuppressWarnings("unused")
    //jni method
    public void d(String s, double b) {
        if (b == -1D) {
            tv.setText(s);
        } else {
            System.out.println("s = " + s);
            System.out.println("b = " + b);
            tv.setText(String.format("%s", b + "%"));
        }
    }

    native int kwmDecode(String f, String dF);
}