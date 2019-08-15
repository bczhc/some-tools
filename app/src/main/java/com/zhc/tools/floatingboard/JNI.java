package com.zhc.tools.floatingboard;

import android.content.Context;

public class JNI {
    static {
        System.loadLibrary("a");
    }

    public native int mG(Context mainActivity, String key);
}