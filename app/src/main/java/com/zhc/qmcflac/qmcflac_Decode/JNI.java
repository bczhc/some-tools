package com.zhc.qmcflac.qmcflac_Decode;

import static com.zhc.qmcflac.MainActivity.tv;

public class JNI {
    static {
        System.loadLibrary("QMC");
    }
    public native int decode(String f, String dF);

    public static void d(String s, double b) {
        if (b == -1D) {
            tv.setText(s);
        } else {
            tv.setText(String.format("%s", b + "%"));
        }
    }
}