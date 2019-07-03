package com.zhc;

import android.widget.TextView;

public class JNI {
    private TextView tv;

    public JNI(TextView tv) {
        this.tv = tv;
    }

    static {
        System.loadLibrary("doJNI");
    }

    public native int qmcDecode(String f, String dF);

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

    public native int kwmDecode(String f, String dF);

    public native int Base128_encode(String f, String dF);

    public native int Base128_decode(String f, String dF);
}