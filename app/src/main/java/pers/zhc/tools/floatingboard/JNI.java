package pers.zhc.tools.floatingboard;

import android.content.Context;

public class JNI {
    static {
        System.loadLibrary("a");
        System.loadLibrary("fb_tools");
    }

    public native int mG(Context mainActivity, String key);

    native byte[] floatToByteArray(float f);

    native byte[] intToByteArray(int i);

    native float byteArrayTofloat(byte[] bytes);

    native int byteArrayToInt(byte[] bytes);
}

